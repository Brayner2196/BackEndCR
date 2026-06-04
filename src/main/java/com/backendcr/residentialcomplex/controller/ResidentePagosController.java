package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.dto.pago.MovimientoCobroDto;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.AbonoService;
import com.backendcr.residentialcomplex.service.CobroService;
import com.backendcr.residentialcomplex.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/residente")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentePagosController {

    private final CobroService cobroService;
    private final PagoService pagoService;
    private final AbonoService abonoService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping("/estado-cuenta")
    public EstadoCuentaResponse estadoCuenta(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) Long propiedadId) {
        return cobroService.estadoCuenta(resolverUsuarioId(email), propiedadId);
    }

    @GetMapping("/cobros")
    public List<CobroResponse> misCobros(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) EstadoCobro estado,
            @RequestParam(required = false) Long propiedadId) {
        Long usuarioId = resolverUsuarioId(email);
        return estado != null
                ? cobroService.listarPorUsuarioYEstado(usuarioId, estado, propiedadId)
                : cobroService.listarPorUsuario(usuarioId, propiedadId);
    }

    @GetMapping("/cobros/historial")
    public List<CobroResponse> historial(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) Long propiedadId) {
        return cobroService.listarPorUsuarioYEstado(resolverUsuarioId(email), EstadoCobro.PAGADO, propiedadId);
    }

    /**
     * Historial paginado filtrado por propiedad activa.
     * GET /api/residente/cobros/historial-paginado?page=0&size=5&propiedadId=X
     */
    @GetMapping("/cobros/historial-paginado")
    public Page<CobroResponse> historialPaginado(
            @AuthenticationPrincipal String email,
            @PageableDefault(size = 5, sort = "fechaGeneracion", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long propiedadId) {
        return cobroService.listarHistorialPaginado(resolverUsuarioId(email), pageable, propiedadId);
    }

    @PostMapping("/pagos")
    public PagoResponse registrarPago(@Valid @RequestBody PagoRequest req,
                                      @AuthenticationPrincipal String email) {
        return pagoService.registrar(req, resolverUsuarioId(email));
    }

    @GetMapping("/pagos")
    public List<PagoResponse> misPagos(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) Long propiedadId) {
        return pagoService.listarPorUsuario(resolverUsuarioId(email), propiedadId);
    }

    /**
     * Retorna todos los movimientos de un cobro específico:
     * pagos directos + movimientos de abonos distribuidos a ese cobro.
     */
    /** Retorna el estado actualizado de un cobro específico del residente. */
    @GetMapping("/cobros/{id}")
    public CobroResponse miCobro(@PathVariable Long id,
                                 @AuthenticationPrincipal String email) {
        return cobroService.getCobroPorIdYUsuario(id, resolverUsuarioId(email));
    }

    @GetMapping("/cobros/{id}/movimientos")
    public List<MovimientoCobroDto> movimientosCobro(@PathVariable Long id) {
        return pagoService.getMovimientosCobro(id);
    }

    // ─── Abonos ───────────────────────────────────────────────────

    @PostMapping("/abonos")
    @ResponseStatus(HttpStatus.CREATED)
    public AbonoResponse registrarAbono(@Valid @RequestBody AbonoRequest req,
                                        @AuthenticationPrincipal String email) {
        return abonoService.registrar(req, resolverUsuarioId(email));
    }

    @GetMapping("/abonos")
    public List<AbonoResponse> misAbonos(@AuthenticationPrincipal String email) {
        return abonoService.listarPorUsuario(resolverUsuarioId(email));
    }

    @GetMapping("/abonos/simular")
    public SimularAbonoResponse simularAbono(
            @RequestParam Long propiedadId,
            @RequestParam BigDecimal monto) {
        return abonoService.simular(propiedadId, monto);
    }

    @GetMapping("/saldo-favor")
    public SaldoFavorResponse saldoFavor(@RequestParam Long propiedadId) {
        return abonoService.consultarSaldoFavor(propiedadId);
    }

    private Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Usuario no encontrado para email=" + email + " tenant=" + tenantId));
    }
}
