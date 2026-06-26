package com.jeevan.core.service;

import com.jeevan.core.config.ClinicProperties;
import com.jeevan.core.dto.response.SlotsResponse;
import com.jeevan.core.exception.ResourceNotFoundException;
import com.jeevan.core.model.DoctorAvailability;
import com.jeevan.core.model.enums.AppointmentStatus;
import com.jeevan.core.model.enums.DayOfWeek;
import com.jeevan.core.repository.AppointmentRepository;
import com.jeevan.core.repository.AvailabilityRepository;
import com.jeevan.core.repository.DoctorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Generates available slots dynamically (no slots table): expand the weekday's
 * availability windows into fixed-length slots, drop already-booked starts, and
 * drop past slots when the date is today. Out-of-window dates yield no slots.
 */
@Service
@Transactional(readOnly = true)
public class SlotService {

    private final DoctorRepository doctors;
    private final AvailabilityRepository availability;
    private final AppointmentRepository appointments;
    private final ClinicProperties clinic;

    public SlotService(DoctorRepository doctors,
                       AvailabilityRepository availability,
                       AppointmentRepository appointments,
                       ClinicProperties clinic) {
        this.doctors = doctors;
        this.availability = availability;
        this.appointments = appointments;
        this.clinic = clinic;
    }

    public SlotsResponse getSlots(Long doctorId, LocalDate date) {
        if (!doctors.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }

        ZoneId zone = ZoneId.of(clinic.clinicTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate maxDate = today.plusDays(clinic.bookingWindowDays());

        // Outside the bookable window → nothing offered.
        if (date.isBefore(today) || date.isAfter(maxDate)) {
            return new SlotsResponse(doctorId, date, List.of());
        }

        DayOfWeek dayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());
        List<DoctorAvailability> windows = availability.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        if (windows.isEmpty()) {
            return new SlotsResponse(doctorId, date, List.of());
        }

        int slotMinutes = clinic.slotLengthMinutes();
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        Set<Instant> taken = new HashSet<>(
                appointments.findActiveStartTimes(doctorId, AppointmentStatus.CANCELLED, dayStart, dayEnd));
        Instant now = Instant.now();
        boolean isToday = date.equals(today);

        List<SlotsResponse.Slot> slots = new ArrayList<>();
        for (DoctorAvailability window : windows) {
            LocalTime cursor = window.getStartTime();
            while (!cursor.plusMinutes(slotMinutes).isAfter(window.getEndTime())) {
                LocalTime slotStart = cursor;
                Instant start = LocalDateTime.of(date, slotStart).atZone(zone).toInstant();
                Instant end = LocalDateTime.of(date, slotStart.plusMinutes(slotMinutes)).atZone(zone).toInstant();

                boolean isPast = isToday && !start.isAfter(now);
                if (!taken.contains(start) && !isPast) {
                    slots.add(new SlotsResponse.Slot(start, end, slotStart.toString()));
                }
                cursor = cursor.plusMinutes(slotMinutes);
            }
        }

        slots.sort(Comparator.comparing(SlotsResponse.Slot::start));
        return new SlotsResponse(doctorId, date, slots);
    }
}
