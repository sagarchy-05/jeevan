package com.jeevan.core.service;

import com.jeevan.core.dto.response.AvailabilityResponse;
import com.jeevan.core.dto.response.DoctorResponse;
import com.jeevan.core.dto.response.PagedResponse;
import com.jeevan.core.exception.ResourceNotFoundException;
import com.jeevan.core.model.Doctor;
import com.jeevan.core.model.DoctorAvailability;
import com.jeevan.core.repository.AvailabilityRepository;
import com.jeevan.core.repository.DoctorRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class DoctorService {

    private static final int DEFAULT_PAGE_SIZE = 9;
    private static final int MAX_PAGE_SIZE = 100;

    private final DoctorRepository doctors;
    private final AvailabilityRepository availability;

    public DoctorService(DoctorRepository doctors, AvailabilityRepository availability) {
        this.doctors = doctors;
        this.availability = availability;
    }

    public PagedResponse<DoctorResponse> listDoctors(String search, String specialty, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by("name").ascending());

        String specialtyFilter = (specialty == null || specialty.isBlank()) ? null : specialty.trim();
        String searchTerm = (search == null || search.isBlank()) ? null : search.trim();

        Page<Doctor> result = doctors.search(specialtyFilter, searchTerm, pageable);
        return PagedResponse.from(result.map(DoctorResponse::from));
    }

    public List<String> getSpecialties() {
        return doctors.findDistinctSpecialties();
    }

    public AvailabilityResponse getAvailability(Long doctorId) {
        Doctor doctor = doctors.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        List<AvailabilityResponse.Window> windows = availability.findByDoctorId(doctorId).stream()
                .sorted(Comparator.comparingInt((DoctorAvailability a) -> a.getDayOfWeek().ordinal())
                        .thenComparing(DoctorAvailability::getStartTime))
                .map(a -> new AvailabilityResponse.Window(
                        a.getDayOfWeek().name(),
                        a.getStartTime().toString(),
                        a.getEndTime().toString()))
                .toList();

        return new AvailabilityResponse(doctor.getId(), doctor.getName(), windows);
    }
}
