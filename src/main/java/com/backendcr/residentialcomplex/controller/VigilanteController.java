package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.config.SecurityUtils;
import com.backendcr.residentialcomplex.dto.cartera.AccesoVehicularResponse;
import com.backendcr.residentialcomplex.dto.cartera.ResultadoRestriccion;
import com.backendcr.residentialcomplex.dto.vigilancia.BitacoraAccesoResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.EntregarPaqueteRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.PaqueteResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.PropiedadOpcionPage;
import com.backendcr.residentialcomplex.dto.vigilancia.RegistrarAccesoPeatonalRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.RegistrarPaqueteRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.DetalleVisitaResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.RechazarVisitaRequest;
import com.backendcr.residentialcomplex.entity.BitacoraAcceso;
import com.backendcr.residentialcomplex.entity.ConfigVigilancia;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Vehiculo;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import com.backendcr.residentialcomplex.entity.enums.ResultadoAcceso;
import com.backendcr.residentialcomplex.entity.enums.TipoEventoAcceso;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.VehiculoRepository;
import com.backendcr.residentialcomplex.service.PropiedadService;
import com.backendcr.residentialcomplex.service.cartera.RestriccionService;
import com.backendcr.residentialcomplex.service.vigilancia.BitacoraService;
import com.backendcr.residentialcomplex.service.vigilancia.ConfigVigilanciaService;
import com.backendcr.residentialcomplex.service.vigilancia.PaqueteService;
import com.backendcr.residentialcomplex.service.vigilancia.VisitaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Endpoints operativos del rol VIGILANTE: control de acceso (vehicular y
 * peatonal), validación de visitas por QR y paquetería. Accesible al vigilante
 * y, como respaldo, al administrador del conjunto.
 */
@RestController
@RequestMapping("/api/vigilante")
@PreAuthorize("hasAnyRole('VIGILANTE', 'PORTERO', 'TENANT_ADMIN')")
@RequiredArgsConstructor
public class VigilanteController {

    private final VehiculoRepository vehiculoRepo;
    private final PropiedadRepository propiedadRepo;
    private final RestriccionService restriccionService;
    private final PropiedadService propiedadService;
    private final BitacoraService bitacoraService;
    private final ConfigVigilanciaService configService;
    private final VisitaService visitaService;
    private final PaqueteService paqueteService;
    private final SecurityUtils securityUtils;

    // ── Acceso vehicular ──────────────────────────────────────────────────────

    /**
     * Valida si un vehículo puede ingresar según el estado de cartera de su
     * propiedad. Una placa no registrada responde 404 (se gestiona como visitante).
     */
    @GetMapping("/acceso-vehicular")
    public AccesoVehicularResponse accesoVehicular(@RequestParam String placa,
                                                   @AuthenticationPrincipal String email) {
        Vehiculo v = vehiculoRepo.findFirstByPlacaIgnoreCase(placa.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Placa no registrada en el conjunto"));

        ResultadoRestriccion r = restriccionService.verificar(
                v.getPropiedadId(), AccionRestringible.ACCESO_VEHICULAR);

        String propiedad = propiedadRepo.findById(v.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse("N/A");

        bitacoraService.registrar(TipoEventoAcceso.ACCESO_VEHICULAR,
                r.permitido() ? ResultadoAcceso.PERMITIDO : ResultadoAcceso.DENEGADO,
                r.permitido() ? "Acceso vehicular permitido" : ("Denegado: " + r.mensaje()),
                v.getPropiedadId(), v.getPlaca(), null, null, vigilanteId(email), null, null);

        return new AccesoVehicularResponse(
                r.permitido(), v.getPlaca(), v.getPropiedadId(), propiedad,
                r.estadoCodigo(), r.estadoNombre(),
                r.permitido() ? "Acceso permitido" : r.mensaje());
    }

    // ── Acceso peatonal (visitante sin pre-registro) ──────────────────────────

    @PostMapping("/acceso-peatonal")
    @ResponseStatus(HttpStatus.CREATED)
    public BitacoraAccesoResponse accesoPeatonal(@Valid @RequestBody RegistrarAccesoPeatonalRequest req,
                                                 @AuthenticationPrincipal String email) {
        ConfigVigilancia cfg = configService.obtener();
        if (cfg.isExigeDocumentoPeatonal() && (req.documento() == null || req.documento().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El documento del visitante es obligatorio");
        }

        Propiedad propiedad = propiedadRepo.findById(req.propiedadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Propiedad no encontrada"));

        ResultadoRestriccion r = restriccionService.verificar(
                req.propiedadId(), AccionRestringible.ACCESO_PEATONAL_VISITANTE);

        String desc = (req.nombreVisitante() != null ? req.nombreVisitante() : "Visitante")
                + (req.motivo() != null ? " — " + req.motivo() : "")
                + (r.permitido() ? "" : (" (denegado: " + r.mensaje() + ")"));

        BitacoraAcceso b = bitacoraService.registrar(TipoEventoAcceso.ACCESO_PEATONAL,
                r.permitido() ? ResultadoAcceso.PERMITIDO : ResultadoAcceso.DENEGADO,
                desc, req.propiedadId(), null, req.documento(), req.nombreVisitante(),
                vigilanteId(email), null, null);

        return BitacoraAccesoResponse.from(b, propiedad.getIdentificador());
    }

    // ── Visitas (validar QR) ──────────────────────────────────────────────────

    /** Consulta (sin mutar estado) los datos de la visita escaneada. */
    @GetMapping("/visitas/{codigo}")
    public DetalleVisitaResponse consultarVisita(@PathVariable String codigo,
                                                 @AuthenticationPrincipal String email) {
        return visitaService.consultar(codigo, vigilanteId(email));
    }

    /** Aprueba el ingreso de la visita (registra INGRESO). */
    @PostMapping("/visitas/{id}/aprobar")
    public DetalleVisitaResponse aprobarVisita(@PathVariable Long id,
                                               @AuthenticationPrincipal String email) {
        return visitaService.aprobar(id, vigilanteId(email));
    }

    /** Rechaza el ingreso con un motivo obligatorio. */
    @PostMapping("/visitas/{id}/rechazar")
    public DetalleVisitaResponse rechazarVisita(@PathVariable Long id,
                                                @Valid @RequestBody RechazarVisitaRequest req,
                                                @AuthenticationPrincipal String email) {
        return visitaService.rechazar(id, req.motivo(), vigilanteId(email));
    }

    // ── Paquetería ────────────────────────────────────────────────────────────

    @PostMapping("/paquetes")
    @ResponseStatus(HttpStatus.CREATED)
    public PaqueteResponse registrarPaquete(@Valid @RequestBody RegistrarPaqueteRequest req,
                                            @AuthenticationPrincipal String email) {
        return paqueteService.registrar(req, vigilanteId(email));
    }

    @GetMapping("/paquetes/pendientes")
    public List<PaqueteResponse> paquetesPendientes() {
        return paqueteService.listarPendientes();
    }

    @GetMapping("/paquetes/propiedad/{propiedadId}")
    public List<PaqueteResponse> paquetesPorPropiedad(@PathVariable Long propiedadId) {
        return paqueteService.listarPorPropiedad(propiedadId);
    }

    @PutMapping("/paquetes/{id}/entregar")
    public PaqueteResponse entregarPaquete(@PathVariable Long id,
                                          @RequestBody(required = false) EntregarPaqueteRequest req,
                                          @AuthenticationPrincipal String email) {
        return paqueteService.entregar(id, req, vigilanteId(email));
    }

    // ── Propiedades (selector para peatonal/paquetes) ─────────────────────────

    /**
     * Selector paginado de unidades FACTURABLES con buscador (por path corto o
     * identificador), para registrar paquetes/visitas.
     */
    @GetMapping("/propiedades")
    public PropiedadOpcionPage propiedades(
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return propiedadService.buscarFacturablesParaSelector(buscar, page, size);
    }

    // ── Bitácora / minuta ─────────────────────────────────────────────────────

    @GetMapping("/bitacora")
    public List<BitacoraAccesoResponse> bitacora(
            @RequestParam(defaultValue = "50") int limite) {
        return bitacoraService.recientes(limite);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    /** Resuelve el id de usuario del vigilante; null si no tiene fila Usuario (p. ej. admin). */
    private Long vigilanteId(String email) {
        try {
            return securityUtils.resolverUsuarioId(email);
        } catch (Exception e) {
            return null;
        }
    }
}
