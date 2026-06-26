package com.jeevan.core.controller;

import com.jeevan.core.dto.response.AvailabilityResponse;
import com.jeevan.core.dto.response.DoctorResponse;
import com.jeevan.core.dto.response.PagedResponse;
import com.jeevan.core.dto.response.SlotsResponse;
import com.jeevan.core.service.DoctorService;
import com.jeevan.core.service.SlotService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final SlotService slotService;

    public DoctorController(DoctorService doctorService, SlotService slotService) {
        this.doctorService = doctorService;
        this.slotService = slotService;
    }

    @GetMapping
    public PagedResponse<DoctorResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String specialty) {
        return doctorService.listDoctors(specialty, page, size);
    }

    @GetMapping("/specialties")
    public List<String> specialties() {
        return doctorService.getSpecialties();
    }

    @GetMapping("/{id}/availability")
    public AvailabilityResponse availability(@PathVariable Long id) {
        return doctorService.getAvailability(id);
    }

    @GetMapping("/{id}/slots")
    public SlotsResponse slots(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return slotService.getSlots(id, date);
    }
}
