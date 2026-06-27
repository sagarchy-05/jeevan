package com.jeevan.core.service;

import com.jeevan.core.dto.request.BookAppointmentRequest;
import com.jeevan.core.dto.response.AppointmentResponse;
import com.jeevan.core.event.AppointmentBookedEvent;
import com.jeevan.core.event.AppointmentCancelledEvent;
import com.jeevan.core.exception.AppointmentNotCancellableException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointments;
    private final AppointmentHistoryRepository history;
    private final DoctorRepository doctors;
    private final SlotService slotService;
    private final ApplicationEventPublisher events;

    public AppointmentService(AppointmentRepository appointments,
                              AppointmentHistoryRepository history,
                              DoctorRepository doctors,
                              SlotService slotService,
                              ApplicationEventPublisher events) {
        this.appointments = appointments;
        this.history = history;
        this.doctors = doctors;
        this.slotService = slotService;
        this.events = events;
    }

    /**
     * Books a slot synchronously and transactionally. Correctness comes from two
     * layers: a pessimistic lock on the doctor row serialises concurrent attempts,
     * and the partial unique index is the authoritative backstop — a violation is
     * translated to a clean {@link SlotAlreadyBookedException} (409).
     */
    @Transactional
    public AppointmentResponse book(User patient, BookAppointmentRequest request) {
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
        // Restart the notification cycle so the cancellation notification round-trip
        // is observable (PENDING -> SENT) rather than swallowed by the idempotency guard.
        appointment.setNotificationStatus(NotificationStatus.PENDING);
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
    public AppointmentResponse getForPatient(Long patientId, Long appointmentId) {
        Appointment appointment = appointments.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", appointmentId));
        Doctor doctor = doctors.findById(appointment.getDoctorId()).orElse(null);
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

    /**
     * Applies a worker notification result to the appointment — the closing half of
     * the round-trip. Idempotent: a duplicate delivery for an already-applied status
     * is a no-op. Unknown appointment ids are logged and ignored (not retried).
     */
    @Transactional
    public void applyNotificationResult(Long appointmentId, String status, String detail) {
        Appointment appointment = appointments.findById(appointmentId).orElse(null);
        if (appointment == null) {
            log.warn("Received notification result for unknown appointment {}", appointmentId);
            return;
        }

        NotificationStatus newStatus = "FAILED".equalsIgnoreCase(status)
                ? NotificationStatus.FAILED
                : NotificationStatus.SENT;
        NotificationStatus oldStatus = appointment.getNotificationStatus();
        if (oldStatus == newStatus) {
            return;
        }

        appointment.setNotificationStatus(newStatus);
        appointments.save(appointment);

        history.save(AppointmentHistory.builder()
                .appointmentId(appointment.getId())
                .oldStatus(oldStatus.name())
                .newStatus(newStatus.name())
                .reason(newStatus == NotificationStatus.SENT ? "notification sent" : "notification failed")
                .build());
    }
}
