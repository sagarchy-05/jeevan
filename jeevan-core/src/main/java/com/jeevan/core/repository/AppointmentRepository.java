package com.jeevan.core.repository;

import com.jeevan.core.model.Appointment;
import com.jeevan.core.model.enums.AppointmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    boolean existsByDoctorIdAndStartTimeAndAppointmentStatusNot(Long doctorId,
                                                                Instant startTime,
                                                                AppointmentStatus excludedStatus);

    List<Appointment> findByPatientIdOrderByStartTimeDesc(Long patientId);

    Optional<Appointment> findByIdAndPatientId(Long id, Long patientId);

    /**
     * Whether the patient already holds a live appointment whose time range overlaps
     * [start, end) — blocks one patient being booked in two places at once.
     */
    @Query("""
            select (count(a) > 0) from Appointment a
            where a.patientId = :patientId
              and a.appointmentStatus <> :excluded
              and a.startTime < :end
              and a.endTime > :start
            """)
    boolean existsActivePatientOverlap(@Param("patientId") Long patientId,
                                       @Param("excluded") AppointmentStatus excluded,
                                       @Param("start") Instant start,
                                       @Param("end") Instant end);

    /**
     * Start times of this doctor's live (non-cancelled) appointments within a
     * half-open instant range — used to drop already-taken slots in slot generation.
     */
    @Query("""
            select a.startTime from Appointment a
            where a.doctorId = :doctorId
              and a.appointmentStatus <> :excluded
              and a.startTime >= :from
              and a.startTime < :to
            """)
    List<Instant> findActiveStartTimes(@Param("doctorId") Long doctorId,
                                       @Param("excluded") AppointmentStatus excluded,
                                       @Param("from") Instant from,
                                       @Param("to") Instant to);
}
