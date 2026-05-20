package com.backendcr.residentialcomplex.tenant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.pasarela.PasarelaConfigRequest;
import com.backendcr.residentialcomplex.dto.pasarela.PasarelaConfigResponse;
import com.backendcr.residentialcomplex.service.pasarela.PasarelaOrchestrator;
import com.backendcr.residentialcomplex.tenant.dto.ActualizarTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantRequest;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantResponse;
import com.backendcr.residentialcomplex.tenant.dto.TenantResponse;
import com.backendcr.residentialcomplex.tenant.service.TenantService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;
    private final PasarelaOrchestrator pasarelaOrchestrator;

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

    // ─── Pasarelas del tenant (SUPER_ADMIN) ───────────────────────────────────

    @GetMapping("/{id}/pasarelas")
    public List<PasarelaConfigResponse> listarPasarelas(@PathVariable Long id) {
        return pasarelaOrchestrator.listarPasarelas(id);
    }

    @PostMapping("/{id}/pasarelas")
    @ResponseStatus(HttpStatus.CREATED)
    public PasarelaConfigResponse crearPasarela(
            @PathVariable Long id,
            @Valid @RequestBody PasarelaConfigRequest request) {
        return pasarelaOrchestrator.crearOActualizarPasarela(id, request);
    }

    @PatchMapping("/{id}/pasarelas/{pasarelaId}/toggle")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void togglePasarela(
            @PathVariable Long id,
            @PathVariable Long pasarelaId,
            @RequestParam boolean activa) {
        pasarelaOrchestrator.toggleActiva(pasarelaId, activa);
    }

    @DeleteMapping("/{id}/pasarelas/{pasarelaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarPasarela(
            @PathVariable Long id,
            @PathVariable Long pasarelaId) {
        pasarelaOrchestrator.eliminarPasarela(pasarelaId);
    }
}
