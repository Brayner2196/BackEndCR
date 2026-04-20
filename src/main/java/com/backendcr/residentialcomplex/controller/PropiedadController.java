package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.propiedad.PropiedadRequest;
import com.backendcr.residentialcomplex.dto.propiedad.PropiedadResponse;
import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.dto.propiedad.UsuarioPropiedadResponse;
import com.backendcr.residentialcomplex.entity.enums.EstadoPropiedad;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.PropiedadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropiedadController {

    private final PropiedadService propiedadService;
    private final UsuarioRepository usuarioRepo;
    private final IdentidadRepository identidadRepo;

    @GetMapping("/api/tipos-propiedad")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN')")
    public List<TipoPropiedadNodoDto> obtenerArbol() {
        return propiedadService.obtenerArbol();
    }

    @PostMapping("/api/tipos-propiedad")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public TipoPropiedadNodoDto crearTipo(@Valid @RequestBody TipoPropiedadNodoDto request) {
        return propiedadService.crearTipo(request);
    }

    @PutMapping("/api/tipos-propiedad/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public TipoPropiedadNodoDto actualizarTipo(@PathVariable Long id,
                                               @Valid @RequestBody TipoPropiedadNodoDto request) {
        return propiedadService.actualizarTipo(id, request);
    }

    @DeleteMapping("/api/tipos-propiedad/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void desactivarTipo(@PathVariable Long id) {
        propiedadService.desactivarTipo(id);
    }

    @GetMapping("/api/propiedades")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<PropiedadResponse> listar() {
        return propiedadService.listarTodas();
    }

    @PostMapping("/api/propiedades")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public PropiedadResponse crear(@Valid @RequestBody PropiedadRequest request) {
        return propiedadService.crear(request);
    }

    @PatchMapping("/api/propiedades/{id}/estado")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public PropiedadResponse actualizarEstado(@PathVariable Long id,
                                              @RequestParam EstadoPropiedad estado) {
        return propiedadService.actualizarEstado(id, estado);
    }

    @PostMapping("/api/propiedades/{propiedadId}/usuarios/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void asignarUsuario(@PathVariable Long propiedadId, @PathVariable Long usuarioId) {
        propiedadService.asignarUsuario(propiedadId, usuarioId);
    }

    @DeleteMapping("/api/propiedades/{propiedadId}/usuarios/{usuarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void quitarUsuario(@PathVariable Long propiedadId, @PathVariable Long usuarioId) {
        propiedadService.quitarUsuario(propiedadId, usuarioId);
    }

    @GetMapping("/api/propiedades/mis-propiedades")
    @PreAuthorize("isAuthenticated()")
    public List<UsuarioPropiedadResponse> misPropiedades(@AuthenticationPrincipal String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(identidad -> usuarioRepo.findByIdentidadId(identidad.getId()))
                .map(usuario -> propiedadService.getMisPropiedades(usuario.getId()))
                .orElse(List.of());
    }
}
