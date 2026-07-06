package com.backendcr.residentialcomplex.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.backendcr.residentialcomplex.dto.consejo.ActaReunionResponse;
import com.backendcr.residentialcomplex.dto.consejo.ActaReunionUpdateRequest;
import com.backendcr.residentialcomplex.service.acta.ActaReunionService;

import lombok.RequiredArgsConstructor;

/**
 * Actas de reunión por voz (Whisper local).
 *
 * Seguridad en dos capas:
 * 1. Rol JWT: se exige ROLE_CONSEJERO (o TENANT_ADMIN solo para lectura).
 * 2. Cargo contra BD: toda operación de escritura exige que el usuario sea el
 *    PRESIDENTE ACTIVO del consejo (PermisoValidator.esPresidenteActivo), de modo
 *    que un token con claims desactualizados no otorga acceso indebido.
 */
@RestController
@RequestMapping("/api/consejo/actas")
@RequiredArgsConstructor
public class ConsejoActaController {

    private final ActaReunionService actaService;

    /** Autorización de escritura: SOLO el presidente activo del consejo. */
    private static final String SOLO_PRESIDENTE =
            "hasRole('CONSEJERO') and @permisoValidator.esPresidenteActivo(authentication.name)";

    // ─── Lectura (consejo completo + admin para supervisión) ─────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('CONSEJERO','TENANT_ADMIN')")
    public List<ActaReunionResponse> listar() {
        return actaService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONSEJERO','TENANT_ADMIN')")
    public ActaReunionResponse obtener(@PathVariable Long id) {
        return actaService.obtener(id);
    }

    // ─── Escritura (solo PRESIDENTE) ──────────────────────────────────────────

    /**
     * Sube la grabación de la reunión y encola la transcripción Whisper.
     * multipart/form-data: audio (archivo), titulo, fechaReunion (ISO-8601 opcional),
     * duracionSegundos (opcional).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize(SOLO_PRESIDENTE)
    @ResponseStatus(HttpStatus.CREATED)
    public ActaReunionResponse crear(
            @RequestPart("audio") MultipartFile audio,
            @RequestParam("titulo") String titulo,
            @RequestParam(value = "fechaReunion", required = false) String fechaReunion,
            @RequestParam(value = "duracionSegundos", required = false) Integer duracionSegundos,
            Principal principal) {
        return actaService.crear(principal.getName(), titulo, fechaReunion, duracionSegundos, audio);
    }

    /** Edita título/contenido de un acta en BORRADOR. */
    @PutMapping("/{id}")
    @PreAuthorize(SOLO_PRESIDENTE)
    public ActaReunionResponse actualizar(@PathVariable Long id,
                                          @RequestBody ActaReunionUpdateRequest req) {
        return actaService.actualizar(id, req);
    }

    /** Cierra el acta: pasa de BORRADOR a FINALIZADA (inmutable). */
    @PostMapping("/{id}/finalizar")
    @PreAuthorize(SOLO_PRESIDENTE)
    public ActaReunionResponse finalizar(@PathVariable Long id) {
        return actaService.finalizar(id);
    }

    /** Reintenta la transcripción de un acta en ERROR. */
    @PostMapping("/{id}/reintentar")
    @PreAuthorize(SOLO_PRESIDENTE)
    public ActaReunionResponse reintentar(@PathVariable Long id) {
        return actaService.reintentar(id);
    }

    /** Elimina un acta en BORRADOR o ERROR (nunca FINALIZADA). */
    @DeleteMapping("/{id}")
    @PreAuthorize(SOLO_PRESIDENTE)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        actaService.eliminar(id);
    }
}
