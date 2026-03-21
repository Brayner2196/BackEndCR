package com.backendcr.residentialcomplex.tenant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.tenant.dto.ActualizarTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantResponse;
import com.backendcr.residentialcomplex.tenant.dto.TenantResponse;
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
    public List<TenantResponse> obtenerTenants() {
        return tenantService.obtenerTenants();
    }

    @GetMapping("/{id}")
    public TenantResponse obtenerPorId(@PathVariable Long id) {
        return tenantService.obtenerPorId(id);
    }

    @PutMapping("/{id}")
    public TenantResponse actualizarTenant(
            @PathVariable Long id,
            @Valid @RequestBody ActualizarTenantRequest request) {
        return tenantService.actualizarTenant(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarTenant(@PathVariable Long id) {
        tenantService.desactivarTenant(id);
    }

    @PatchMapping("/{id}/activar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activarTenant(@PathVariable Long id) {
        tenantService.activarTenant(id);
    }
}
