package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.consejo.MiembroConsejoResponse;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.service.ConsejoService;
import com.backendcr.residentialcomplex.service.PQRService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints exclusivos para el rol CONSEJERO.
 * Accesibles también por TENANT_ADMIN (para pruebas y supervisión).
 */
@RestController
@RequestMapping("/api/consejo")
@RequiredArgsConstructor
public class ConsejoController {

    private final ConsejoService consejoService;
    private final PQRService pqrService;

    /**
     * Directorio público del consejo — cualquier residente autenticado puede verlo.
     */
    @GetMapping("/miembros")
    @PreAuthorize("hasAnyRole('PROPIETARIO','INQUILINO','CONSEJERO','TENANT_ADMIN')")
    public List<MiembroConsejoResponse> directorio() {
        return consejoService.directorioPublico();
    }

    /**
     * Vista de todas las PQRs del conjunto (no solo las propias).
     * Permite al consejo hacer seguimiento de las peticiones de los residentes.
     */
    @GetMapping("/pqrs")
    @PreAuthorize("hasAnyRole('CONSEJERO','TENANT_ADMIN')")
    public List<PQRResponse> todasLasPqrs(
            @RequestParam(required = false) String estado) {
        return pqrService.listarTodasParaConsejo(estado);
    }
}
