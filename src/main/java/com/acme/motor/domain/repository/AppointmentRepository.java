package com.acme.motor.domain.repository;

import com.acme.motor.domain.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByConversationIdOrderByCreatedAtAsc(Long conversationId);
}
