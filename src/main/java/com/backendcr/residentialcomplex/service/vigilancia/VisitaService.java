package com.backendcr.residentialcomplex.service.vigilancia;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.dto.cartera.ResultadoRestriccion;
import com.backendcr.residentialcomplex.dto.vigilancia.CrearVisitaRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.DetalleVisitaResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.VisitaResponse;
import com.backendcr.residentialcomplex.entity.ConfigVigilancia;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Visita;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import com.backendcr.residentialcomplex.entity.enums.EstadoVisita;
import com.backendcr.residentialcomplex.entity.enums.ResultadoAcceso;
import com.backendcr.residentialcomplex.entity.enums.TipoEventoAcceso;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.repository.VisitaRepository;
import com.backendcr.residentialcomplex.service.NotificacionService;
import com.backendcr.residentialcomplex.service.cartera.RestriccionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Base64;
import java.time.temporal.ChronoUnit;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica de visitas pre-registradas, generación del QR (datos embebidos) y
 * decisión del vigilante (consultar / aprobar / rechazar).
 */
@Service
@RequiredArgsConstructor
public class VisitaService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final VisitaRepository visitaRepo;
    private final PropiedadRepository propiedadRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final RestriccionService restriccionService;
    private final BitacoraService bitacoraService;
    private final ConfigVigilanciaService configService;
    private final NotificacionService notificacionService;

    // ── Residente ───────────────────────────────────────────────────────────

    @Transactional
    public VisitaResponse crear(CrearVisitaRequest req, Long residenteId) {
        if (!usuarioPropiedadRepo.existsByUsuarioIdAndPropiedadId(residenteId, req.propiedadId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La propiedad no pertenece al residente");
        }
        ConfigVigilancia cfg = configService.obtener();

        Instant franjaDesde = TenantClock.aInstante(req.franjaDesde());
        Instant franjaHasta = TenantClock.aInstante(req.franjaHasta());

        // Vigencia del QR: si hay horario, vence al final de la franja; si no,
        // usa validezHoras explícitas o la parametrización del conjunto.
        Instant expira;
        if (franjaHasta != null) {
            expira = franjaHasta;
        } else {
            int horas = req.validezHoras() != null ? req.validezHoras() : cfg.getExpiracionVisitaHoras();
            expira = TenantClock.ahora().plus(horas, ChronoUnit.HOURS);
        }

        Visita v = new Visita();
        v.setCodigo(generarCodigoUnico());
        v.setNombreVisitante(req.nombreVisitante());
        v.setDocumento(req.documento());
        v.setPlaca(req.placa());
        v.setMotivo(req.motivo());
        v.setCantidadPersonas(req.cantidadPersonas() != null && req.cantidadPersonas() > 0
                ? req.cantidadPersonas() : 1);
        v.setAcompanantes(req.acompanantes());
        v.setFranjaDesde(franjaDesde);
        v.setFranjaHasta(franjaHasta);
        v.setPropiedadId(req.propiedadId());
        v.setResidenteId(residenteId);
        v.setEstado(EstadoVisita.PENDIENTE);
        v.setExpiraEn(expira);
        return toResponse(visitaRepo.save(v));
    }

    public List<VisitaResponse> listarMias(Long residenteId) {
        return visitaRepo.findAllByResidenteIdOrderByCreadoEnDesc(residenteId)
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public VisitaResponse cancelar(Long visitaId, Long residenteId) {
        Visita v = visitaRepo.findById(visitaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));
        if (!v.getResidenteId().equals(residenteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No puedes cancelar esta visita");
        }
        if (v.getEstado() != EstadoVisita.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden cancelar visitas pendientes");
        }
        v.setEstado(EstadoVisita.CANCELADA);
        return toResponse(visitaRepo.save(v));
    }

    // ── Vigilante: consultar (solo lectura) ──────────────────────────────────

    @Transactional
    public DetalleVisitaResponse consultar(String codigo, Long vigilanteId) {
        Visita v = visitaRepo.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Código de visita no válido"));

        // Expiración perezosa: si está pendiente y ya pasó su vigencia → VENCIDA.
        if (v.getEstado() == EstadoVisita.PENDIENTE && v.getExpiraEn().isBefore(TenantClock.ahora())) {
            v.setEstado(EstadoVisita.VENCIDA);
            visitaRepo.save(v);
        }
        return toDetalle(v);
    }

    // ── Vigilante: aprobar ────────────────────────────────────────────────────

    @Transactional
    public DetalleVisitaResponse aprobar(Long visitaId, Long vigilanteId) {
        Visita v = obtenerDecidible(visitaId);

        ResultadoRestriccion r = restriccionService.verificar(
                v.getPropiedadId(), AccionRestringible.ACCESO_PEATONAL_VISITANTE);
        if (!r.permitido() && !configService.obtener().isPermitirAprobarConCarteraRestringida()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    r.mensaje() != null ? r.mensaje()
                            : "La unidad está restringida por cartera; no se permite aprobar el ingreso");
        }

        v.setEstado(EstadoVisita.INGRESO);
        v.setIngresoEn(TenantClock.ahora());
        v.setValidadaPor(vigilanteId);
        visitaRepo.save(v);

        String ident = identificador(v);
        bitacoraService.registrar(TipoEventoAcceso.VISITA_VALIDADA, ResultadoAcceso.PERMITIDO,
                "Ingreso autorizado de " + v.getNombreVisitante()
                        + " (" + v.getCantidadPersonas() + " pers.)",
                v.getPropiedadId(), v.getPlaca(), v.getDocumento(), v.getNombreVisitante(),
                vigilanteId, v.getId(), null);

        notificar(v, "Tu visita ingresó",
                v.getNombreVisitante() + " registró ingreso a " + ident);

        return toDetalle(v);
    }

    // ── Vigilante: rechazar ──────────────────────────────────────────────────

    @Transactional
    public DetalleVisitaResponse rechazar(Long visitaId, String motivo, Long vigilanteId) {
        Visita v = obtenerDecidible(visitaId);

        v.setEstado(EstadoVisita.RECHAZADA);
        v.setMotivoRechazo(motivo);
        v.setValidadaPor(vigilanteId);
        visitaRepo.save(v);

        bitacoraService.registrar(TipoEventoAcceso.VISITA_VALIDADA, ResultadoAcceso.DENEGADO,
                "Ingreso rechazado de " + v.getNombreVisitante() + ": " + motivo,
                v.getPropiedadId(), v.getPlaca(), v.getDocumento(), v.getNombreVisitante(),
                vigilanteId, v.getId(), null);

        notificar(v, "Visita rechazada",
                "Portería rechazó a " + v.getNombreVisitante() + ". Motivo: " + motivo);

        return toDetalle(v);
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private Visita obtenerDecidible(Long visitaId) {
        Visita v = visitaRepo.findById(visitaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visita no encontrada"));
        if (v.getEstado() == EstadoVisita.PENDIENTE
                && v.getExpiraEn().isBefore(TenantClock.ahora())) {
            v.setEstado(EstadoVisita.VENCIDA);
            visitaRepo.save(v);
        }
        if (v.getEstado() != EstadoVisita.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La visita ya no está pendiente (estado: " + v.getEstado() + ")");
        }
        return v;
    }

    private DetalleVisitaResponse toDetalle(Visita v) {
        String ident = identificador(v);
        ResultadoRestriccion r = restriccionService.verificar(
                v.getPropiedadId(), AccionRestringible.ACCESO_PEATONAL_VISITANTE);
        boolean restringida = !r.permitido();
        boolean politicaOverride = configService.obtener().isPermitirAprobarConCarteraRestringida();
        boolean puedeDecidir = v.getEstado() == EstadoVisita.PENDIENTE;
        boolean puedeAprobar = puedeDecidir && (!restringida || politicaOverride);

        String mensaje;
        if (!puedeDecidir) {
            mensaje = switch (v.getEstado()) {
                case INGRESO -> "La visita ya registró ingreso";
                case RECHAZADA -> "La visita fue rechazada";
                case CANCELADA -> "La visita fue cancelada por el residente";
                case VENCIDA -> "El QR de la visita está vencido";
                default -> "La visita ya fue procesada";
            };
        } else if (restringida) {
            mensaje = r.mensaje() != null ? r.mensaje() : "Unidad restringida por cartera";
        } else {
            mensaje = "Visita válida";
        }

        return new DetalleVisitaResponse(
                v.getId(), v.getCodigo(), v.getEstado(), v.getNombreVisitante(),
                v.getCantidadPersonas(), v.getAcompanantes(), v.getDocumento(), v.getPlaca(),
                v.getMotivo(), v.getPropiedadId(), ident, v.getFranjaDesde(), v.getFranjaHasta(),
                v.getExpiraEn(), v.getIngresoEn(), v.getMotivoRechazo(),
                restringida, restringida ? r.mensaje() : null, puedeDecidir, puedeAprobar, mensaje);
    }

    private void notificar(Visita v, String titulo, String cuerpo) {
        try {
            notificacionService.enviarAUsuario(v.getResidenteId(), titulo, cuerpo,
                    Map.of("tipo", "VISITA", "visitaId", String.valueOf(v.getId())));
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private String identificador(Visita v) {
        return propiedadRepo.findById(v.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse("N/A");
    }

    private String generarCodigoUnico() {
        String codigo;
        do {
            codigo = UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 10).toUpperCase();
        } while (visitaRepo.existsByCodigoIgnoreCase(codigo));
        return codigo;
    }

    private VisitaResponse toResponse(Visita v) {
        String ident = propiedadRepo.findById(v.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse(null);
        return VisitaResponse.from(v, ident, construirQrPayload(v, ident));
    }

    /**
     * Payload del QR con datos embebidos (base64 de un JSON). El vigilante lo
     * decodifica para mostrarlo; la decisión real la resuelve el servidor por código.
     */
    private String construirQrPayload(Visita v, String ident) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("v", 1);
            data.put("c", v.getCodigo());
            data.put("n", v.getNombreVisitante());
            data.put("q", v.getCantidadPersonas());
            data.put("a", v.getAcompanantes());
            data.put("u", ident);
            data.put("d", v.getFranjaDesde() != null ? v.getFranjaDesde().toString() : null);
            data.put("h", v.getFranjaHasta() != null ? v.getFranjaHasta().toString() : null);
            byte[] json = MAPPER.writeValueAsBytes(data);
            return "CRV1:" + Base64.getEncoder().encodeToString(json);
        } catch (Exception e) {
            return v.getCodigo();
        }
    }
}
