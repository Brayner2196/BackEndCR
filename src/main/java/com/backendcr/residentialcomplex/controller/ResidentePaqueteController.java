package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.vigilancia.PaqueteResponse;
import com.backendcr.residentialcomplex.service.vigilancia.PaqueteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Paquetería del residente: ver la correspondencia recibida para sus propiedades.
 */
@RestController
@RequestMapping("/api/residente/paquetes")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidentePaqueteController {

    private final PaqueteService paqueteService;
    private final SecurityUtils securityUtils;

    @GetMapping("/me")
    public List<PaqueteResponse> mios(@AuthenticationPrincipal String email) {
        return paqueteService.listarMios(securityUtils.resolverUsuarioId(email));
    }
}
