package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.vigilancia.CrearVisitaRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.VisitaResponse;
import com.backendcr.residentialcomplex.service.vigilancia.VisitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Visitas del residente: pre-registro con generación de QR, consulta y cancelación.
 */
@RestController
@RequestMapping("/api/residente/visitas")
@PreAuthorize("hasAnyRole('PROPIETARIO', 'INQUILINO')")
@RequiredArgsConstructor
public class ResidenteVisitaController {

    private final VisitaService visitaService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitaResponse crear(@Valid @RequestBody CrearVisitaRequest req,
                                @AuthenticationPrincipal String email) {
        return visitaService.crear(req, securityUtils.resolverUsuarioId(email));
    }

    @GetMapping("/me")
    public List<VisitaResponse> mias(@AuthenticationPrincipal String email) {
        return visitaService.listarMias(securityUtils.resolverUsuarioId(email));
    }

    @PutMapping("/{id}/cancelar")
    public VisitaResponse cancelar(@PathVariable Long id,
                                   @AuthenticationPrincipal String email) {
        return visitaService.cancelar(id, securityUtils.resolverUsuarioId(email));
    }
}
