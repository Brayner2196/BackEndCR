package com.backendcr.residentialcomplex.tenant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantResponse;
import com.backendcr.residentialcomplex.tenant.service.TenantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CrearTenantResponse crearTenant(@Valid @RequestBody CrearTenantRequest request) {
        return tenantService.crearTenant(request);
    }

    @GetMapping
    public List<Tenant> obtenerTenants() {
        return tenantService.obtenerTenants();
    }
}
