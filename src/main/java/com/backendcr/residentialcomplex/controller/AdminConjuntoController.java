package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.conjunto.MiConjuntoRequest;
import com.backendcr.residentialcomplex.dto.conjunto.MiConjuntoResponse;
import com.backendcr.residentialcomplex.service.AdminConjuntoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mi-conjunto")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminConjuntoController {

    private final AdminConjuntoService service;

    @GetMapping
    public MiConjuntoResponse obtener() {
        return service.obtener();
    }

    @PatchMapping
    public MiConjuntoResponse actualizar(@Valid @RequestBody MiConjuntoRequest req) {
        return service.actualizar(req);
    }
}
