package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.publicacion.PublicacionResponse;
import com.backendcr.residentialcomplex.service.PublicacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminPublicacionController {

    private final PublicacionService publicacionService;

    @GetMapping("/publicaciones")
    public List<PublicacionResponse> listar() {
        return publicacionService.listarTodasAdmin();
    }

    @DeleteMapping("/publicaciones/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        publicacionService.eliminarAdmin(id);
    }
}
