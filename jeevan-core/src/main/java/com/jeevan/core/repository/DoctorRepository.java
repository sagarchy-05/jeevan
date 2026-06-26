package com.jeevan.core.repository;

import com.jeevan.core.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Page<Doctor> findBySpecialty(String specialty, Pageable pageable);

    @Query("select distinct d.specialty from Doctor d order by d.specialty")
    List<String> findDistinctSpecialties();
}
