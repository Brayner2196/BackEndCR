package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.cartera.AccesoVehicularResponse;
import com.backendcr.residentialcomplex.dto.cartera.ResultadoRestriccion;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Vehiculo;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.VehiculoRepository;
import com.backendcr.residentialcomplex.service.cartera.RestriccionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/**
 * Endpoints para el vigilante/portería.
 *
 * NOTA: cuando exista el rol VIGILANTE, restringir con
 * {@code @PreAuthorize("hasRole('VIGILANTE')")}. Por ahora queda accesible a
 * usuarios autenticados del conjunto.
 */
@RestController
@RequestMapping("/api/vigilante")
@RequiredArgsConstructor
public class VigilanteController {

    private final VehiculoRepository vehiculoRepo;
    private final PropiedadRepository propiedadRepo;
    private final RestriccionService restriccionService;

    /**
     * Valida si un vehículo puede ingresar según el estado de cartera de su
     * propiedad. Una placa no registrada responde 404 (el portero la gestiona
     * como visitante).
     */
    @GetMapping("/acceso-vehicular")
    public AccesoVehicularResponse accesoVehicular(@RequestParam String placa) {
        Vehiculo v = vehiculoRepo.findFirstByPlacaIgnoreCase(placa.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Placa no registrada en el conjunto"));

        ResultadoRestriccion r = restriccionService.verificar(
                v.getPropiedadId(), AccionRestringible.ACCESO_VEHICULAR);

        String propiedad = propiedadRepo.findById(v.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse("N/A");

        return new AccesoVehicularResponse(
                r.permitido(),
                v.getPlaca(),
                v.getPropiedadId(),
                propiedad,
                r.estadoCodigo(),
                r.estadoNombre(),
                r.permitido() ? "Acceso permitido" : r.mensaje());
    }
}
