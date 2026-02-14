package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.ResidentialComplexDTO;
import com.backendcr.residentialcomplex.service.ResidentialComplexService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complexes")
@RequiredArgsConstructor
public class ResidentialComplexController {
    
    private final ResidentialComplexService service;
    
    @GetMapping
    public ResponseEntity<List<ResidentialComplexDTO>> getAllComplexes() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ResidentialComplexDTO> getComplexById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    
    @GetMapping("/search")
    public ResponseEntity<List<ResidentialComplexDTO>> searchComplexes(
            @RequestParam String name) {
        return ResponseEntity.ok(service.searchByName(name));
    }
    
    @PostMapping
    public ResponseEntity<ResidentialComplexDTO> createComplex(
            @Valid @RequestBody ResidentialComplexDTO dto) {
        ResidentialComplexDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ResidentialComplexDTO> updateComplex(
            @PathVariable Long id,
            @Valid @RequestBody ResidentialComplexDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComplex(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
