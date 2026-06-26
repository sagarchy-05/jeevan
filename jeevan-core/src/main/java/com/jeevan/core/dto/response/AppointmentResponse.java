package com.jeevan.core.dto.response;

import com.jeevan.core.model.Appointment;
import com.jeevan.core.model.Doctor;

import java.time.Instant;

public record AppointmentResponse(
        Long id,
        Long doctorId,
        String doctorName,
        String specialty,
        Instant startTime,
        Instant endTime,
        String appointmentStatus,
        String notificationStatus,
        Instant createdAt
) {
    public static AppointmentResponse from(Appointment appointment, Doctor doctor) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getDoctorId(),
                doctor != null ? doctor.getName() : null,
                doctor != null ? doctor.getSpecialty() : null,
                appointment.getStartTime(),
                appointment.getEndTime(),
                appointment.getAppointmentStatus().name(),
                appointment.getNotificationStatus().name(),
                appointment.getCreatedAt());
    }
}
