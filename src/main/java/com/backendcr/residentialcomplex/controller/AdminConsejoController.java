package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.consejo.MiembroConsejoRequest;
import com.backendcr.residentialcomplex.dto.consejo.MiembroConsejoResponse;
import com.backendcr.residentialcomplex.service.ConsejoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD del consejo comunal — solo TENANT_ADMIN.
 * Designa, actualiza y revoca consejeros.
 */
@RestController
@RequestMapping("/api/admin/consejo")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminConsejoController {

    private final ConsejoService consejoService;

    @GetMapping
    public List<MiembroConsejoResponse> listarActivos() {
        return consejoService.listarActivos();
    }

    @GetMapping("/historial")
    public List<MiembroConsejoResponse> historial() {
        return consejoService.listarHistorial();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MiembroConsejoResponse designar(@Valid @RequestBody MiembroConsejoRequest req) {
        return consejoService.designar(req);
    }

    @PutMapping("/{id}")
    public MiembroConsejoResponse actualizar(@PathVariable Long id,
                                              @Valid @RequestBody MiembroConsejoRequest req) {
        return consejoService.actualizar(id, req);
    }

    /** Revoca la membresía (activo=false). No elimina el registro histórico. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revocar(@PathVariable Long id) {
        consejoService.revocar(id);
    }
}
