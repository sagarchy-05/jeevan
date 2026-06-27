package com.jeevan.core.repository;

import com.jeevan.core.model.Doctor;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    /**
     * Server-side doctor search. {@code specialty} (when present) is an exact filter;
     * {@code search} (when present) matches name OR specialty, case-insensitive,
     * substring. Either may be null.
     */
    @Query("""
            select d from Doctor d
            where (:specialty is null or d.specialty = :specialty)
              and (:search is null
                   or lower(d.name) like lower(concat('%', cast(:search as string), '%'))
                   or lower(d.specialty) like lower(concat('%', cast(:search as string), '%')))
            """)
    Page<Doctor> search(@Param("specialty") String specialty,
                        @Param("search") String search,
                        Pageable pageable);

    @Query("select distinct d.specialty from Doctor d order by d.specialty")
    List<String> findDistinctSpecialties();

    /**
     * Loads the doctor row with a pessimistic write lock (SELECT … FOR UPDATE).
     * Serialises concurrent booking transactions for the same doctor so the second
     * transaction observes the first's committed appointment before it inserts.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);
}
