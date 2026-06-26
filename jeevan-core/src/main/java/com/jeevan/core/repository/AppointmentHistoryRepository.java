package com.jeevan.core.repository;

import com.jeevan.core.model.AppointmentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentHistoryRepository extends JpaRepository<AppointmentHistory, Long> {

    List<AppointmentHistory> findByAppointmentIdOrderByChangedAtAsc(Long appointmentId);
}
