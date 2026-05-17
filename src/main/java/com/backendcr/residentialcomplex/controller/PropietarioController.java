package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.inquilino.CrearInquilinoRequest;
import com.backendcr.residentialcomplex.dto.inquilino.PermisoInquilinoDto;
import com.backendcr.residentialcomplex.dto.usuario.UsuarioResponse;
import com.backendcr.residentialcomplex.service.PropietarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints exclusivos para el rol PROPIETARIO.
 * Permite gestionar los inquilinos de su propia unidad (apto + torre)
 * y otorgar/revocar permisos sobre ellos.
 */
@RestController
@RequestMapping("/api/propietario/inquilinos")
@PreAuthorize("hasRole('PROPIETARIO')")
@RequiredArgsConstructor
public class PropietarioController {

    private final PropietarioService propietarioService;

    /** Lista los inquilinos de la unidad del propietario autenticado. */
    @GetMapping
    public List<UsuarioResponse> listarInquilinos() {
        return propietarioService.listarInquilinos();
    }

    /** Crea un nuevo inquilino en la unidad del propietario autenticado. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse crearInquilino(@Valid @RequestBody CrearInquilinoRequest request) {
        return propietarioService.crearInquilino(request);
    }

    /** Elimina un inquilino y toda su información (permisos, propiedades, identidad). */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarInquilino(@PathVariable Long id) {
        propietarioService.eliminarInquilino(id);
    }

    /** Retorna los permisos activos del inquilino. */
    @GetMapping("/{id}/permisos")
    public PermisoInquilinoDto listarPermisos(@PathVariable Long id) {
        return new PermisoInquilinoDto(propietarioService.listarPermisos(id));
    }

    /**
     * Reemplaza todos los permisos del inquilino con los indicados.
     * Enviar lista vacía revoca todos los permisos.
     */
    @PutMapping("/{id}/permisos")
    public PermisoInquilinoDto actualizarPermisos(
            @PathVariable Long id,
            @RequestBody PermisoInquilinoDto dto) {
        return new PermisoInquilinoDto(propietarioService.actualizarPermisos(id, dto.permisos()));
    }
}
