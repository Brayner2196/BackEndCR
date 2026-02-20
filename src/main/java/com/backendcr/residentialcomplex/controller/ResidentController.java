package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.ResidentDTO;
import com.backendcr.residentialcomplex.service.ResidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residents")
@RequiredArgsConstructor
public class ResidentController {

    @Autowired
    private final ResidentService service;

    @GetMapping
    public ResponseEntity<List<ResidentDTO>> getAllResidents() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResidentDTO> getResidentById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping("/unit/{unitId}")
    public ResponseEntity<List<ResidentDTO>> getResidentsByUnitId(
            @PathVariable Long unitId) {
        return ResponseEntity.ok(service.findByUnitId(unitId));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ResidentDTO>> searchResidents(
            @RequestParam String lastName) {
        return ResponseEntity.ok(service.searchByLastName(lastName));
    }

    @PostMapping
    public ResponseEntity<ResidentDTO> createResident(
            @Valid @RequestBody ResidentDTO dto) {
        ResidentDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResidentDTO> updateResident(
            @PathVariable Long id,
            @Valid @RequestBody ResidentDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResident(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
