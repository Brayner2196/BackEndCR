package com.backendcr.residentialcomplex.service.vigilancia;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.dto.cartera.ResultadoRestriccion;
import com.backendcr.residentialcomplex.dto.vigilancia.CrearVisitaRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.ValidarVisitaResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lógica de visitas pre-registradas y validación del QR en portería.
 */
@Service
@RequiredArgsConstructor
public class VisitaService {

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
        int horas = req.validezHoras() != null ? req.validezHoras() : cfg.getExpiracionVisitaHoras();

        Visita v = new Visita();
        v.setCodigo(generarCodigoUnico());
        v.setNombreVisitante(req.nombreVisitante());
        v.setDocumento(req.documento());
        v.setPlaca(req.placa());
        v.setMotivo(req.motivo());
        v.setPropiedadId(req.propiedadId());
        v.setResidenteId(residenteId);
        v.setEstado(EstadoVisita.PENDIENTE);
        v.setExpiraEn(TenantClock.ahora().plus(horas, ChronoUnit.HOURS));
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

    // ── Vigilante ───────────────────────────────────────────────────────────

    @Transactional
    public ValidarVisitaResponse validar(String codigo, Long vigilanteId) {
        Visita v = visitaRepo.findByCodigoIgnoreCase(codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Código de visita no válido"));

        String ident = propiedadRepo.findById(v.getPropiedadId())
                .map(Propiedad::getIdentificador).orElse("N/A");

        // Estados terminales o no autorizables
        if (v.getEstado() == EstadoVisita.CANCELADA) {
            return negativa(v, ident, "La visita fue cancelada por el residente");
        }
        if (v.getEstado() == EstadoVisita.INGRESO || v.getEstado() == EstadoVisita.FINALIZADA) {
            return negativa(v, ident, "La visita ya registró ingreso");
        }
        if (v.getExpiraEn().isBefore(TenantClock.ahora())) {
            v.setEstado(EstadoVisita.VENCIDA);
            visitaRepo.save(v);
            return negativa(v, ident, "El QR de la visita está vencido");
        }

        // Restricción por cartera de la propiedad de destino
        ResultadoRestriccion r = restriccionService.verificar(
                v.getPropiedadId(), AccionRestringible.ACCESO_PEATONAL_VISITANTE);
        if (!r.permitido()) {
            bitacoraService.registrar(TipoEventoAcceso.VISITA_VALIDADA, ResultadoAcceso.DENEGADO,
                    "Acceso denegado por cartera: " + r.mensaje(), v.getPropiedadId(), v.getPlaca(),
                    v.getDocumento(), v.getNombreVisitante(), vigilanteId, v.getId(), null);
            return new ValidarVisitaResponse(false, v.getEstado(), v.getNombreVisitante(),
                    v.getDocumento(), v.getPlaca(), v.getPropiedadId(), ident,
                    r.mensaje() != null ? r.mensaje() : "Acceso restringido por estado de cartera");
        }

        // Autorizar ingreso
        v.setEstado(EstadoVisita.INGRESO);
        v.setIngresoEn(TenantClock.ahora());
        v.setValidadaPor(vigilanteId);
        visitaRepo.save(v);

        bitacoraService.registrar(TipoEventoAcceso.VISITA_VALIDADA, ResultadoAcceso.PERMITIDO,
                "Ingreso autorizado de " + v.getNombreVisitante(), v.getPropiedadId(), v.getPlaca(),
                v.getDocumento(), v.getNombreVisitante(), vigilanteId, v.getId(), null);

        notificarResidente(v, ident);

        return new ValidarVisitaResponse(true, EstadoVisita.INGRESO, v.getNombreVisitante(),
                v.getDocumento(), v.getPlaca(), v.getPropiedadId(), ident, "Acceso autorizado");
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private ValidarVisitaResponse negativa(Visita v, String ident, String mensaje) {
        return new ValidarVisitaResponse(false, v.getEstado(), v.getNombreVisitante(),
                v.getDocumento(), v.getPlaca(), v.getPropiedadId(), ident, mensaje);
    }

    private void notificarResidente(Visita v, String ident) {
        try {
            notificacionService.enviarAUsuario(v.getResidenteId(),
                    "Tu visita ingresó",
                    v.getNombreVisitante() + " registró ingreso a " + ident,
                    Map.of("tipo", "VISITA", "visitaId", String.valueOf(v.getId())));
        } catch (Exception ignored) {
            // La notificación es best-effort; no debe tumbar la validación.
        }
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
        return VisitaResponse.from(v, ident);
    }
}
