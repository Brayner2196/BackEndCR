package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.propiedad.PropiedadRequest;
import com.backendcr.residentialcomplex.dto.propiedad.PropiedadResponse;
import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.dto.propiedad.UsuarioPropiedadResponse;
import com.backendcr.residentialcomplex.dto.propiedad.ValorTipoPropiedadDto;
import com.backendcr.residentialcomplex.entity.enums.EstadoPropiedad;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.PropiedadService;
import com.backendcr.residentialcomplex.service.ValorTipoPropiedadService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class PropiedadController {

    private final PropiedadService propiedadService;
    private final ValorTipoPropiedadService valorService;
    private final UsuarioRepository usuarioRepo;
    private final IdentidadRepository identidadRepo;

    @GetMapping("/api/tipos-propiedad")
    @PreAuthorize("hasAnyRole('TENANT_ADMIN')")
    public List<TipoPropiedadNodoDto> obtenerArbol(
            @RequestParam(defaultValue = "false") boolean soloFacturables) {
        return propiedadService.obtenerArbol(soloFacturables);
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

    // ── Valores permitidos por tipo (catálogo) ────────────────────────────────

    /** Valores resueltos (híbrido) para alimentar un dropdown de un nivel. */
    @GetMapping("/api/tipos-propiedad/{tipoId}/valores")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<ValorTipoPropiedadDto> valoresPermitidos(
            @PathVariable Long tipoId,
            @RequestParam(required = false) Long parentValorId) {
        return valorService.resolverPermitidos(tipoId, parentValorId);
    }

    /** Todos los valores del tipo (activos e inactivos), para la gestión. */
    @GetMapping("/api/tipos-propiedad/{tipoId}/valores/todos")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<ValorTipoPropiedadDto> valoresTodos(@PathVariable Long tipoId) {
        return valorService.listarTodos(tipoId);
    }

    @PostMapping("/api/tipos-propiedad/{tipoId}/valores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ValorTipoPropiedadDto crearValor(@PathVariable Long tipoId,
                                            @Valid @RequestBody ValorTipoPropiedadDto request) {
        return valorService.crear(tipoId, request);
    }

    @PutMapping("/api/valores-propiedad/{id}")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public ValorTipoPropiedadDto actualizarValor(@PathVariable Long id,
                                                 @Valid @RequestBody ValorTipoPropiedadDto request) {
        return valorService.actualizar(id, request);
    }

    @DeleteMapping("/api/valores-propiedad/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void desactivarValor(@PathVariable Long id) {
        valorService.desactivar(id);
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

    @PatchMapping("/api/propiedades/{propiedadId}/usuarios/{usuarioId}/principal")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public void marcarComoPrincipal(@PathVariable Long propiedadId, @PathVariable Long usuarioId) {
        propiedadService.marcarComoPrincipal(propiedadId, usuarioId);
    }

    @GetMapping("/api/usuarios/{usuarioId}/propiedades")
    @PreAuthorize("hasRole('TENANT_ADMIN')")
    public List<UsuarioPropiedadResponse> propiedadesDeUsuario(@PathVariable Long usuarioId) {
        return propiedadService.getMisPropiedades(usuarioId);
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
