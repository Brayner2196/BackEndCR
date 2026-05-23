package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.presupuesto.*;
import com.backendcr.residentialcomplex.service.PresupuestoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/presupuestos")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminPresupuestoController {

    private final PresupuestoService presupuestoService;
    private final SecurityUtils securityUtils;

    // ── Presupuestos ──────────────────────────────────────────────────────────

    @GetMapping
    public List<PresupuestoResponse> listar() {
        return presupuestoService.listarTodos();
    }

    @GetMapping("/{id}")
    public PresupuestoResponse detalle(@PathVariable Long id) {
        return presupuestoService.obtenerDetalle(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresupuestoResponse crear(@Valid @RequestBody PresupuestoRequest req) {
        return presupuestoService.crear(req);
    }

    @PutMapping("/{id}")
    public PresupuestoResponse actualizar(
            @PathVariable Long id,
            @Valid @RequestBody PresupuestoRequest req) {
        return presupuestoService.actualizar(id, req);
    }

    /** Activa o desactiva un presupuesto. Body: {"activo": true} */
    @PatchMapping("/{id}/activo")
    public PresupuestoResponse toggleActivo(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body) {
        boolean activo = Boolean.TRUE.equals(body.get("activo"));
        return presupuestoService.toggleActivo(id, activo);
    }

    // ── Gastos ────────────────────────────────────────────────────────────────

    @PostMapping("/{id}/gastos")
    @ResponseStatus(HttpStatus.CREATED)
    public GastoRegistradoResponse registrarGasto(
            @PathVariable Long id,
            @Valid @RequestBody GastoRegistradoRequest req,
            @AuthenticationPrincipal String email) {
        Long adminId = securityUtils.resolverUsuarioId(email);
        return presupuestoService.registrarGasto(id, req, adminId);
    }

    @DeleteMapping("/{id}/gastos/{gastoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarGasto(
            @PathVariable Long id,
            @PathVariable Long gastoId) {
        presupuestoService.eliminarGasto(id, gastoId);
    }
}
