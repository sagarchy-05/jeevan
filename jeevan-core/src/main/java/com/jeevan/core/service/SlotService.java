package com.jeevan.core.service;

import com.jeevan.core.config.ClinicProperties;
import com.jeevan.core.dto.response.SlotsResponse;
import com.jeevan.core.exception.DoctorNotAvailableException;
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
 * availability windows into fixed-length slots, drop past slots when the date is
 * today, and (for the public endpoint) drop already-booked starts. The same
 * structural generation backs booking validation, so what we offer and what we
 * accept can never drift.
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

    /** Available slots for the public endpoint: structural slots minus already-booked. */
    public SlotsResponse getSlots(Long doctorId, LocalDate date) {
        if (!doctors.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }
        List<Candidate> candidates = structuralCandidates(doctorId, date);
        if (candidates.isEmpty()) {
            return new SlotsResponse(doctorId, date, List.of());
        }

        ZoneId zone = ZoneId.of(clinic.clinicTimezone());
        Instant dayStart = date.atStartOfDay(zone).toInstant();
        Instant dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant();
        Set<Instant> taken = new HashSet<>(
                appointments.findActiveStartTimes(doctorId, AppointmentStatus.CANCELLED, dayStart, dayEnd));

        List<SlotsResponse.Slot> slots = candidates.stream()
                .filter(c -> !taken.contains(c.start()))
                .map(c -> new SlotsResponse.Slot(c.start(), c.end(), c.localStart().toString()))
                .sorted(Comparator.comparing(SlotsResponse.Slot::start))
                .toList();

        return new SlotsResponse(doctorId, date, slots);
    }

    /**
     * Confirms {@code start} is a structurally valid slot for the doctor (within an
     * availability window, aligned to the slot grid, inside the booking window, not
     * past) and returns its end instant. Does NOT check whether it is already taken —
     * that is the booking transaction's job (lock + unique index). Throws
     * {@link DoctorNotAvailableException} otherwise.
     */
    public Instant validateBookableSlot(Long doctorId, Instant start) {
        ZoneId zone = ZoneId.of(clinic.clinicTimezone());
        LocalDate date = start.atZone(zone).toLocalDate();
        return structuralCandidates(doctorId, date).stream()
                .filter(c -> c.start().equals(start))
                .map(Candidate::end)
                .findFirst()
                .orElseThrow(() -> new DoctorNotAvailableException(
                        "The selected time is not an available slot for this doctor."));
    }

    /** Structurally valid slots for a doctor+date (availability + window + not-past), ignoring bookings. */
    private List<Candidate> structuralCandidates(Long doctorId, LocalDate date) {
        ZoneId zone = ZoneId.of(clinic.clinicTimezone());
        LocalDate today = LocalDate.now(zone);
        LocalDate maxDate = today.plusDays(clinic.bookingWindowDays());
        if (date.isBefore(today) || date.isAfter(maxDate)) {
            return List.of();
        }

        DayOfWeek dayOfWeek = DayOfWeek.valueOf(date.getDayOfWeek().name());
        List<DoctorAvailability> windows = availability.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
        if (windows.isEmpty()) {
            return List.of();
        }

        int slotMinutes = clinic.slotLengthMinutes();
        Instant now = Instant.now();
        boolean isToday = date.equals(today);

        List<Candidate> candidates = new ArrayList<>();
        for (DoctorAvailability window : windows) {
            LocalTime cursor = window.getStartTime();
            while (!cursor.plusMinutes(slotMinutes).isAfter(window.getEndTime())) {
                LocalTime slotStart = cursor;
                Instant start = LocalDateTime.of(date, slotStart).atZone(zone).toInstant();
                Instant end = LocalDateTime.of(date, slotStart.plusMinutes(slotMinutes)).atZone(zone).toInstant();
                boolean isPast = isToday && !start.isAfter(now);
                if (!isPast) {
                    candidates.add(new Candidate(slotStart, start, end));
                }
                cursor = cursor.plusMinutes(slotMinutes);
            }
        }
        return candidates;
    }

    private record Candidate(LocalTime localStart, Instant start, Instant end) {
    }
}
