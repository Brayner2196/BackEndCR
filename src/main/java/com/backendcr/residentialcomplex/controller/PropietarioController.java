package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.dto.inquilino.CrearInquilinoRequest;
import com.backendcr.residentialcomplex.dto.usuario.UsuarioResponse;
import com.backendcr.residentialcomplex.service.PropietarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints exclusivos para el rol PROPIETARIO.
 * Permite gestionar los inquilinos de su propia unidad (apto + torre).
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

    /** Elimina un inquilino de la unidad del propietario autenticado. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarInquilino(@PathVariable Long id) {
        propietarioService.eliminarInquilino(id);
    }
}
