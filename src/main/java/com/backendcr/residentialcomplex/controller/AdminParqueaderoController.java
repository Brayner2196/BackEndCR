package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.parqueadero.*;
import com.backendcr.residentialcomplex.dto.vehiculo.DecisionVehiculoRequest;
import com.backendcr.residentialcomplex.dto.vehiculo.VehiculoResponse;
import com.backendcr.residentialcomplex.service.ConfiguracionParqueaderoService;
import com.backendcr.residentialcomplex.service.ParqueaderoService;
import com.backendcr.residentialcomplex.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/parqueaderos")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminParqueaderoController {

    private final ConfiguracionParqueaderoService configService;
    private final ParqueaderoService parqueaderoService;
    private final VehiculoService vehiculoService;

    // ─── Configuración ─────────────────────────────────────────

    @GetMapping("/configuracion")
    public ConfiguracionParqueaderoResponse obtenerConfig() {
        return configService.obtener();
    }

    @PutMapping("/configuracion")
    public ConfiguracionParqueaderoResponse guardarConfig(
            @Valid @RequestBody ConfiguracionParqueaderoRequest req) {
        return configService.guardar(req);
    }

    // ─── Parqueaderos ──────────────────────────────────────────

    // Solo existen parqueaderos PRIVADOS como registros individuales.
    // Los comunales son un conteo en /configuracion.
    @GetMapping
    public List<ParqueaderoResponse> listar() {
        return parqueaderoService.listarTodos();
    }

    @PostMapping("/bulk")
    @ResponseStatus(HttpStatus.CREATED)
    public ParqueaderoBulkResultado crearBulk(
            @Valid @RequestBody ParqueaderoBulkRequest req) {
        return parqueaderoService.crearBulk(req);
    }

    @PatchMapping("/{id}/propiedad")
    public ParqueaderoResponse asignarPropiedad(
            @PathVariable Long id,
            @RequestBody AsignarPropiedadRequest req) {
        return parqueaderoService.asignarPropiedad(id, req.propiedadId());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        parqueaderoService.eliminar(id);
    }

    // ─── Vehículos pendientes ──────────────────────────────────

    @GetMapping("/vehiculos")
    public List<VehiculoResponse> listarVehiculos(
            @RequestParam(required = false) Boolean soloPendientes) {
        return Boolean.TRUE.equals(soloPendientes)
                ? vehiculoService.listarPendientes()
                : vehiculoService.listarTodos();
    }

    @PutMapping("/vehiculos/{id}/aprobar")
    public VehiculoResponse aprobar(@PathVariable Long id) {
        return vehiculoService.aprobar(id);
    }

    @PutMapping("/vehiculos/{id}/rechazar")
    public VehiculoResponse rechazar(@PathVariable Long id,
                                     @RequestBody(required = false) DecisionVehiculoRequest req) {
        return vehiculoService.rechazar(id, req);
    }
}
