package com.jeevan.core.service;

import com.jeevan.core.dto.request.BookAppointmentRequest;
import com.jeevan.core.dto.response.AppointmentResponse;
import com.jeevan.core.event.AppointmentBookedEvent;
import com.jeevan.core.event.AppointmentCancelledEvent;
import com.jeevan.core.exception.AppointmentNotCancellableException;
import com.jeevan.core.exception.EmailNotVerifiedException;
import com.jeevan.core.exception.PatientDoubleBookingException;
import com.jeevan.core.exception.ResourceNotFoundException;
import com.jeevan.core.exception.SlotAlreadyBookedException;
import com.jeevan.core.model.Appointment;
import com.jeevan.core.model.AppointmentHistory;
import com.jeevan.core.model.Doctor;
import com.jeevan.core.model.User;
import com.jeevan.core.model.enums.AppointmentStatus;
import com.jeevan.core.model.enums.NotificationStatus;
import com.jeevan.core.repository.AppointmentHistoryRepository;
import com.jeevan.core.repository.AppointmentRepository;
import com.jeevan.core.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository appointments;
    private final AppointmentHistoryRepository history;
    private final DoctorRepository doctors;
    private final SlotService slotService;
    private final ApplicationEventPublisher events;
    private final boolean emailVerificationEnabled;

    public AppointmentService(AppointmentRepository appointments,
                              AppointmentHistoryRepository history,
                              DoctorRepository doctors,
                              SlotService slotService,
                              ApplicationEventPublisher events,
                              @Value("${jeevan.email.verification-enabled:false}") boolean emailVerificationEnabled) {
        this.appointments = appointments;
        this.history = history;
        this.doctors = doctors;
        this.slotService = slotService;
        this.events = events;
        this.emailVerificationEnabled = emailVerificationEnabled;
    }

    /**
     * Books a slot synchronously and transactionally. Correctness comes from two
     * layers: a pessimistic lock on the doctor row serialises concurrent attempts,
     * and the partial unique index is the authoritative backstop — a violation is
     * translated to a clean {@link SlotAlreadyBookedException} (409).
     */
    @Transactional
    public AppointmentResponse book(User patient, BookAppointmentRequest request) {
        if (emailVerificationEnabled && !patient.isEmailVerified()) {
            throw new EmailNotVerifiedException();
        }

        // Lock first: any other booking transaction for this doctor now waits here.
        Doctor doctor = doctors.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", request.doctorId()));

        Instant start = request.startTime();
        Instant end = slotService.validateBookableSlot(doctor.getId(), start);

        if (appointments.existsActivePatientOverlap(
                patient.getId(), AppointmentStatus.CANCELLED, start, end)) {
            throw new PatientDoubleBookingException();
        }

        // Reliable under the doctor lock: the winner's row is already committed/visible.
        if (appointments.existsByDoctorIdAndStartTimeAndAppointmentStatusNot(
                doctor.getId(), start, AppointmentStatus.CANCELLED)) {
            throw new SlotAlreadyBookedException();
        }

        Appointment appointment = Appointment.builder()
                .patientId(patient.getId())
                .doctorId(doctor.getId())
                .startTime(start)
                .endTime(end)
                .appointmentStatus(AppointmentStatus.CONFIRMED)
                .notificationStatus(NotificationStatus.PENDING)
                .build();

        try {
            appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            // Partial unique index fired — authoritative duplicate guard.
            throw new SlotAlreadyBookedException();
        }

        history.save(AppointmentHistory.builder()
                .appointmentId(appointment.getId())
                .oldStatus(null)
                .newStatus(AppointmentStatus.CONFIRMED.name())
                .reason("booked")
                .build());

        events.publishEvent(new AppointmentBookedEvent(
                UUID.randomUUID().toString(), "APPOINTMENT_BOOKED",
                appointment.getId(), patient.getId(), patient.getEmail(), patient.getFullName(),
                doctor.getId(), doctor.getName(), doctor.getSpecialty(),
                appointment.getStartTime(), appointment.getEndTime(), Instant.now()));

        return AppointmentResponse.from(appointment, doctor);
    }

    /**
     * Cancels the patient's own appointment, but only before it starts. Sets status
     * CANCELLED (which frees the slot for rebooking — the partial unique index and
     * slot queries both ignore cancelled rows) and writes a history row. The
     * {@code appointment.cancelled} event is published after commit in step 7.
     */
    @Transactional
    public AppointmentResponse cancel(User patient, Long appointmentId) {
        Appointment appointment = appointments.findByIdAndPatientId(appointmentId, patient.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));

        if (appointment.getAppointmentStatus() != AppointmentStatus.CONFIRMED) {
            throw new AppointmentNotCancellableException("This appointment is already cancelled.");
        }
        if (!appointment.getStartTime().isAfter(Instant.now())) {
            throw new AppointmentNotCancellableException(
                    "An appointment can only be cancelled before it starts.");
        }

        appointment.setAppointmentStatus(AppointmentStatus.CANCELLED);
        appointments.save(appointment);

        history.save(AppointmentHistory.builder()
                .appointmentId(appointment.getId())
                .oldStatus(AppointmentStatus.CONFIRMED.name())
                .newStatus(AppointmentStatus.CANCELLED.name())
                .reason("cancelled by patient")
                .build());

        Doctor doctor = doctors.findById(appointment.getDoctorId()).orElse(null);

        events.publishEvent(new AppointmentCancelledEvent(
                UUID.randomUUID().toString(), "APPOINTMENT_CANCELLED",
                appointment.getId(), patient.getId(), patient.getEmail(), patient.getFullName(),
                appointment.getDoctorId(),
                doctor != null ? doctor.getName() : null,
                doctor != null ? doctor.getSpecialty() : null,
                appointment.getStartTime(), appointment.getEndTime(), Instant.now()));

        return AppointmentResponse.from(appointment, doctor);
    }

    @Transactional(readOnly = true)
    public List<AppointmentResponse> listForPatient(Long patientId) {
        List<Appointment> patientAppointments = appointments.findByPatientIdOrderByStartTimeDesc(patientId);
        if (patientAppointments.isEmpty()) {
            return List.of();
        }
        List<Long> doctorIds = patientAppointments.stream()
                .map(Appointment::getDoctorId)
                .distinct()
                .toList();
        Map<Long, Doctor> doctorsById = doctors.findAllById(doctorIds).stream()
                .collect(Collectors.toMap(Doctor::getId, Function.identity()));
        return patientAppointments.stream()
                .map(a -> AppointmentResponse.from(a, doctorsById.get(a.getDoctorId())))
                .toList();
    }
}
