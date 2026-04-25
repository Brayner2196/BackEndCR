package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.pago.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import com.backendcr.residentialcomplex.service.CobroService;
import com.backendcr.residentialcomplex.service.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/residente")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentePagosController {

    private final CobroService cobroService;
    private final PagoService pagoService;
    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;

    @GetMapping("/estado-cuenta")
    public EstadoCuentaResponse estadoCuenta(@AuthenticationPrincipal String email) {
        return cobroService.estadoCuenta(resolverUsuarioId(email));
    }

    @GetMapping("/cobros")
    public List<CobroResponse> misCobros(
            @AuthenticationPrincipal String email,
            @RequestParam(required = false) EstadoCobro estado) {
        Long usuarioId = resolverUsuarioId(email);
        return estado != null
                ? cobroService.listarPorUsuarioYEstado(usuarioId, estado)
                : cobroService.listarPorUsuario(usuarioId);
    }

    @GetMapping("/cobros/historial")
    public List<CobroResponse> historial(@AuthenticationPrincipal String email) {
        return cobroService.listarPorUsuarioYEstado(resolverUsuarioId(email), EstadoCobro.PAGADO);
    }

    @PostMapping("/pagos")
    public PagoResponse registrarPago(@Valid @RequestBody PagoRequest req,
                                      @AuthenticationPrincipal String email) {
        return pagoService.registrar(req, resolverUsuarioId(email));
    }

    @GetMapping("/pagos")
    public List<PagoResponse> misPagos(@AuthenticationPrincipal String email) {
        return pagoService.listarPorUsuario(resolverUsuarioId(email));
    }

    private Long resolverUsuarioId(String email) {
        String tenantId = TenantContext.getTenant();
        return identidadRepo.findByEmailAndTenantId(email, tenantId)
                .flatMap(i -> usuarioRepo.findByIdentidadId(i.getId()))
                .map(u -> u.getId())
                .orElseThrow();
    }
}
