package com.jeevan.core.controller;

import com.jeevan.core.dto.request.BookAppointmentRequest;
import com.jeevan.core.dto.response.AppointmentResponse;
import com.jeevan.core.security.AppUserDetails;
import com.jeevan.core.service.AppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> book(@AuthenticationPrincipal AppUserDetails principal,
                                                    @Valid @RequestBody BookAppointmentRequest request) {
        AppointmentResponse response = appointmentService.book(principal.getUser(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public List<AppointmentResponse> myAppointments(@AuthenticationPrincipal AppUserDetails principal) {
        return appointmentService.listForPatient(principal.getId());
    }

    @PostMapping("/{id}/cancel")
    public AppointmentResponse cancel(@AuthenticationPrincipal AppUserDetails principal,
                                      @PathVariable Long id) {
        return appointmentService.cancel(principal.getUser(), id);
    }
}
