package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.usuario.ActualizarUsuarioRequest;
import com.backendcr.residentialcomplex.dto.usuario.CrearUsuarioRequest;
import com.backendcr.residentialcomplex.dto.usuario.UsuarioResponse;
import com.backendcr.residentialcomplex.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

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

    @PutMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse aprobar(@PathVariable Long id) {
        return usuarioService.aprobar(id);
    }

    @PutMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public UsuarioResponse rechazar(@PathVariable Long id) {
        return usuarioService.rechazar(id);
    }
}
