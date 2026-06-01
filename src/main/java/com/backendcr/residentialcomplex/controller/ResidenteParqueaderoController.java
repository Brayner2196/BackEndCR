package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.parqueadero.AsignarVehiculoRequest;
import com.backendcr.residentialcomplex.dto.parqueadero.ParqueaderoResponse;
import com.backendcr.residentialcomplex.dto.vehiculo.VehiculoRequest;
import com.backendcr.residentialcomplex.dto.vehiculo.VehiculoResponse;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.service.ParqueaderoService;
import com.backendcr.residentialcomplex.service.VehiculoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/residente/parqueaderos")
@PreAuthorize("hasAnyRole('PROPIETARIO','INQUILINO')")
@RequiredArgsConstructor
public class ResidenteParqueaderoController {

    private final VehiculoService vehiculoService;
    private final ParqueaderoService parqueaderoService;
    private final SecurityUtils securityUtils;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;

    // ─── Vehículos ─────────────────────────────────────────────

    @GetMapping("/vehiculos")
    public List<VehiculoResponse> misVehiculos(
            @RequestParam Long propiedadId,
            @AuthenticationPrincipal String email) {
        validarPropiedadDelUsuario(email, propiedadId);
        return vehiculoService.listarPorPropiedad(propiedadId);
    }

    @PostMapping("/vehiculos")
    @ResponseStatus(HttpStatus.CREATED)
    public VehiculoResponse registrarVehiculo(
            @Valid @RequestBody VehiculoRequest req,
            @RequestParam Long propiedadId,
            @AuthenticationPrincipal String email) {
        validarPropiedadDelUsuario(email, propiedadId);
        return vehiculoService.registrar(req, propiedadId);
    }

    @DeleteMapping("/vehiculos/{vehiculoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarVehiculo(
            @PathVariable Long vehiculoId,
            @RequestParam Long propiedadId,
            @AuthenticationPrincipal String email) {
        validarPropiedadDelUsuario(email, propiedadId);
        vehiculoService.eliminar(vehiculoId, propiedadId);
    }

    // ─── Parqueaderos privados ─────────────────────────────────

    @GetMapping("/mis-parqueaderos")
    public List<ParqueaderoResponse> misParqueaderos(
            @RequestParam Long propiedadId,
            @AuthenticationPrincipal String email) {
        validarPropiedadDelUsuario(email, propiedadId);
        return parqueaderoService.listarPorPropiedad(propiedadId);
    }

    @PatchMapping("/{parqueaderoId}/vehiculo")
    public ParqueaderoResponse cambiarVehiculo(
            @PathVariable Long parqueaderoId,
            @RequestBody AsignarVehiculoRequest req,
            @RequestParam Long propiedadId,
            @AuthenticationPrincipal String email) {
        validarPropiedadDelUsuario(email, propiedadId);
        return parqueaderoService.asignarVehiculo(parqueaderoId, req.vehiculoId(), propiedadId);
    }

    // ─── Helper ────────────────────────────────────────────────

    private void validarPropiedadDelUsuario(String email, Long propiedadId) {
        Long usuarioId = securityUtils.resolverUsuarioId(email);
        boolean esDelUsuario = usuarioPropiedadRepo
                .findByUsuarioIdAndPropiedadId(usuarioId, propiedadId)
                .isPresent();
        if (!esDelUsuario) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "No tienes acceso a esta propiedad");
        }
    }
}
