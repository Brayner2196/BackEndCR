package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.AbonoService;
import com.backendcr.residentialcomplex.service.CobroService;
import com.backendcr.residentialcomplex.service.ConfiguracionCuotaService;
import com.backendcr.residentialcomplex.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class AdminPagosController {

    private final CobroService cobroService;
    private final PagoService pagoService;
    private final AbonoService abonoService;
    private final ConfiguracionCuotaService cuotaService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    // ─── Cuotas ────────────────────────────

    /** Solo cuotas activas. */
    @GetMapping("/cuotas")
    public List<ConfiguracionCuotaResponse> listarCuotas() {
        return cuotaService.listar();
    }

    /** Histórico completo de cuotas (activas e inactivas), del más reciente al más antiguo. */
    @GetMapping("/cuotas/historico")
    public List<ConfiguracionCuotaResponse> historicoCuotas() {
        return cuotaService.listarHistorico();
    }

    @PostMapping("/cuotas")
    @ResponseStatus(HttpStatus.CREATED)
    public ConfiguracionCuotaResponse crearCuota(@Valid @RequestBody ConfiguracionCuotaRequest req) {
        return cuotaService.crear(req);
    }

    @PutMapping("/cuotas/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivarCuota(@PathVariable Long id) {
        cuotaService.desactivar(id);
    }

    // ─── Períodos ──────────────────────────

    @GetMapping("/cobros/periodos")
    public List<PeriodoCobroResponse> listarPeriodos() {
        return cobroService.listarPeriodos();
    }

    @PostMapping("/cobros/periodos")
    @ResponseStatus(HttpStatus.CREATED)
    public PeriodoCobroResponse abrirPeriodo(@Valid @RequestBody PeriodoCobroRequest req) {
        return cobroService.abrirPeriodo(req);
    }

    @PutMapping("/cobros/periodos/{id}/cerrar")
    public PeriodoCobroResponse cerrarPeriodo(@PathVariable Long id) {
        return cobroService.cerrarPeriodo(id);
    }

    // ─── Cobros ───────────────────────────

    @PostMapping("/cobros/generar/{anio}/{mes}")
    @ResponseStatus(HttpStatus.CREATED)
    public List<CobroResponse> generarCobros(@PathVariable int anio, @PathVariable int mes) {
        return cobroService.generarCobros(anio, mes);
    }

    @GetMapping("/cobros")
    public List<CobroResponse> listarCobros(
            @RequestParam(required = false) Long periodoId,
            @RequestParam(required = false) EstadoCobro estado) {
        if (periodoId != null) return cobroService.listarPorPeriodo(periodoId);
        if (estado != null) return cobroService.listarPorEstado(estado);
        return cobroService.listarPorEstado(EstadoCobro.PENDIENTE);
    }

    /** Cobros especiales (multas, sanciones, etc.) sin período de cobro asociado. */
    @GetMapping("/cobros/especiales")
    public List<CobroResponse> listarCobrosEspeciales() {
        return cobroService.listarEspeciales();
    }

    /**
     * Crea un cobro especial (multa, sanción, recargo, etc.) sobre una propiedad.
     * No requiere período abierto.
     */
    @PostMapping("/cobros/especiales")
    @ResponseStatus(HttpStatus.CREATED)
    public CobroResponse crearCobroEspecial(@Valid @RequestBody CobroEspecialRequest req,
                                             @AuthenticationPrincipal String email) {
        return cobroService.crearCobroEspecial(req, resolverAdminId(email));
    }

    @PutMapping("/cobros/{id}/exonerar")
    public CobroResponse exonerar(@PathVariable Long id,
                                  @Valid @RequestBody ExonerarCobroRequest req,
                                  @AuthenticationPrincipal String email) {
        return cobroService.exonerar(id, req, resolverAdminId(email));
    }

    // ─── Pagos ────────────────────────────

    @GetMapping("/pagos")
    public List<PagoResponse> listarPagos(
            @RequestParam(defaultValue = "PENDIENTE_VERIFICACION") EstadoPago estado) {
        return pagoService.listarPorEstado(estado);
    }

    @PutMapping("/pagos/{id}/verificar")
    public PagoResponse verificar(@PathVariable Long id,
                                  @RequestBody VerificarPagoRequest req,
                                  @AuthenticationPrincipal String email) {
        return pagoService.verificar(id, req, resolverAdminId(email));
    }

    @PutMapping("/pagos/{id}/rechazar")
    public PagoResponse rechazar(@PathVariable Long id,
                                 @Valid @RequestBody RechazarPagoRequest req,
                                 @AuthenticationPrincipal String email) {
        return pagoService.rechazar(id, req, resolverAdminId(email));
    }

    // ─── Abonos ───────────────────────────────────────────────────

    @GetMapping("/abonos")
    public List<AbonoResponse> listarAbonos(
            @RequestParam(defaultValue = "PENDIENTE_VERIFICACION") EstadoPago estado) {
        return abonoService.listarPorEstado(estado);
    }

    @PutMapping("/abonos/{id}/verificar")
    public AbonoResponse verificarAbono(@PathVariable Long id,
                                        @RequestBody VerificarPagoRequest req,
                                        @AuthenticationPrincipal String email) {
        return abonoService.verificar(id, req, resolverAdminId(email));
    }

    @PutMapping("/abonos/{id}/rechazar")
    public AbonoResponse rechazarAbono(@PathVariable Long id,
                                       @Valid @RequestBody RechazarPagoRequest req,
                                       @AuthenticationPrincipal String email) {
        return abonoService.rechazar(id, req, resolverAdminId(email));
    }

    private Long resolverAdminId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
