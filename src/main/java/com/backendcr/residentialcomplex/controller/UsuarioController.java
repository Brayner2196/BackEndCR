package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.inquilino.CrearInquilinoRequest;
import com.backendcr.residentialcomplex.dto.usuario.ActualizarUsuarioRequest;
import com.backendcr.residentialcomplex.dto.usuario.CrearUsuarioRequest;
import com.backendcr.residentialcomplex.dto.usuario.UsuarioResponse;
import com.backendcr.residentialcomplex.service.PropietarioService;
import com.backendcr.residentialcomplex.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PropietarioService propietarioService;

    @GetMapping
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<UsuarioResponse> listar() {
        return usuarioService.listarTodos();
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<UsuarioResponse> listarPendientes() {
        return usuarioService.listarPendientes();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse crear(@Valid @RequestBody CrearUsuarioRequest request) {
        return usuarioService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarUsuarioRequest request) {
        return usuarioService.actualizar(id, request);
    }

    /**
     * Aprueba un usuario pendiente.
     * @param rolDestino  Rol a asignar: "PROPIETARIO" o "INQUILINO" (default: PROPIETARIO)
     */
    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse aprobar(
            @PathVariable Long id,
            @RequestParam(defaultValue = "PROPIETARIO") String rolDestino) {
        return usuarioService.aprobar(id, rolDestino);
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse rechazar(@PathVariable Long id) {
        return usuarioService.rechazar(id);
    }

    // ── Activar / Desactivar acceso ───────────────────────────────────────────

    @PatchMapping("/{id}/activar")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse activar(@PathVariable Long id,
                                   @AuthenticationPrincipal String email) {
        return usuarioService.activar(id, email);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse desactivar(@PathVariable Long id,
                                      @AuthenticationPrincipal String email) {
        return usuarioService.desactivar(id, email);
    }

    // ── Cambiar rol ───────────────────────────────────────────────────────────

    @PatchMapping("/{id}/rol")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse cambiarRol(@PathVariable Long id,
                                      @RequestParam String rol,
                                      @AuthenticationPrincipal String email) {
        return usuarioService.cambiarRol(id, rol, email);
    }

    /**
     * El admin crea un inquilino para un propietario específico.
     * El inquilino hereda la unidad del propietario indicado.
     */
    @PostMapping("/{propietarioId}/inquilinos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse crearInquilinoParaPropietario(
            @PathVariable Long propietarioId,
            @Valid @RequestBody CrearInquilinoRequest request) {
        return propietarioService.crearInquilinoComoAdmin(propietarioId, request);
    }
}
