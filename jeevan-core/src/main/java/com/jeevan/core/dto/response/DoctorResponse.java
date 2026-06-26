package com.jeevan.core.dto.response;

import com.jeevan.core.model.Doctor;

public record DoctorResponse(
        Long id,
        String name,
        String specialty,
        String bio
) {
    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(doctor.getId(), doctor.getName(), doctor.getSpecialty(), doctor.getBio());
    }
}
