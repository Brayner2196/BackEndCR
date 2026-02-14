package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.ResidentialUnitDTO;
import com.backendcr.residentialcomplex.service.ResidentialUnitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class ResidentialUnitController {
    
    private final ResidentialUnitService service;
    
    @GetMapping
    public ResponseEntity<List<ResidentialUnitDTO>> getAllUnits() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResidentialUnitDTO> getUnitById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping("/complex/{complexId}")
    public ResponseEntity<List<ResidentialUnitDTO>> getUnitsByComplexId(
            @PathVariable Long complexId) {
        return ResponseEntity.ok(service.findByComplexId(complexId));
    }
    
    @PostMapping
    public ResponseEntity<ResidentialUnitDTO> createUnit(
            @Valid @RequestBody ResidentialUnitDTO dto) {
        ResidentialUnitDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ResidentialUnitDTO> updateUnit(
            @PathVariable Long id,
            @Valid @RequestBody ResidentialUnitDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUnit(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
