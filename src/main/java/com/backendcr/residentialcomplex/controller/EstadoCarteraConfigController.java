package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.cartera.EstadoCarteraConfigRequest;
import com.backendcr.residentialcomplex.dto.cartera.EstadoCarteraConfigResponse;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import com.backendcr.residentialcomplex.service.cartera.EstadoCarteraConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Configuración de estados de cartera del conjunto (admin).
 * Estados + reglas + restricciones se guardan como un payload anidado.
 */
@RestController
@RequestMapping("/api/admin/cartera")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class EstadoCarteraConfigController {

    private final EstadoCarteraConfigService service;

    @GetMapping("/estados")
    public List<EstadoCarteraConfigResponse> listar() {
        return service.listar();
    }

    @PostMapping("/estados")
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoCarteraConfigResponse crear(@Valid @RequestBody EstadoCarteraConfigRequest req) {
        return service.crear(req);
    }

    @PutMapping("/estados/{id}")
    public EstadoCarteraConfigResponse actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody EstadoCarteraConfigRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/estados/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }

    /** Siembra el set de estados por defecto (solo si está vacío). */
    @PostMapping("/seed")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EstadoCarteraConfigResponse> seed() {
        return service.sembrarDefaults();
    }

    /** Catálogo de acciones que un estado puede restringir (para la UI). */
    @GetMapping("/acciones")
    public AccionRestringible[] acciones() {
        return service.accionesDisponibles();
    }
}
