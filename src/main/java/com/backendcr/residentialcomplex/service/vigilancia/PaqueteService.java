package com.backendcr.residentialcomplex.service.vigilancia;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.dto.vigilancia.EntregarPaqueteRequest;
import com.backendcr.residentialcomplex.dto.vigilancia.PaqueteResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.RegistrarPaqueteRequest;
import com.backendcr.residentialcomplex.entity.ConfigVigilancia;
import com.backendcr.residentialcomplex.entity.Paquete;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;
import com.backendcr.residentialcomplex.entity.enums.EstadoPaquete;
import com.backendcr.residentialcomplex.entity.enums.ResultadoAcceso;
import com.backendcr.residentialcomplex.entity.enums.TipoEventoAcceso;
import com.backendcr.residentialcomplex.repository.PaqueteRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * Gestión de correspondencia/paquetería en portería.
 */
@Service
@RequiredArgsConstructor
public class PaqueteService {

    private final PaqueteRepository paqueteRepo;
    private final PropiedadRepository propiedadRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final BitacoraService bitacoraService;
    private final ConfigVigilanciaService configService;
    private final NotificacionService notificacionService;

    // ── Vigilante ───────────────────────────────────────────────────────────

    @Transactional
    public PaqueteResponse registrar(RegistrarPaqueteRequest req, Long vigilanteId) {
        Propiedad propiedad = propiedadRepo.findById(req.propiedadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Propiedad no encontrada"));

        Paquete p = new Paquete();
        p.setPropiedadId(req.propiedadId());
        p.setDescripcion(req.descripcion());
        p.setRemitente(req.remitente());
        p.setTransportadora(req.transportadora());
        p.setEstado(EstadoPaquete.RECIBIDO);
        p.setRecibidoPor(vigilanteId);
        Paquete guardado = paqueteRepo.save(p);

        bitacoraService.registrar(TipoEventoAcceso.PAQUETE_RECIBIDO, ResultadoAcceso.INFORMATIVO,
                "Paquete recibido: " + req.descripcion(), req.propiedadId(), null, null, null,
                vigilanteId, null, guardado.getId());

        ConfigVigilancia cfg = configService.obtener();
        if (cfg.isNotificarLlegadaPaquete()) {
            notificarResidentes(req.propiedadId(), propiedad.getIdentificador());
        }

        return PaqueteResponse.from(guardado, propiedad.getIdentificador());
    }

    @Transactional
    public PaqueteResponse entregar(Long paqueteId, EntregarPaqueteRequest req, Long vigilanteId) {
        Paquete p = paqueteRepo.findById(paqueteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paquete no encontrado"));
        if (p.getEstado() == EstadoPaquete.ENTREGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El paquete ya fue entregado");
        }
        p.setEstado(EstadoPaquete.ENTREGADO);
        p.setEntregadoEn(TenantClock.ahora());
        p.setEntregadoA(req != null ? req.entregadoA() : null);
        p.setEntregadoPor(vigilanteId);
        Paquete guardado = paqueteRepo.save(p);

        bitacoraService.registrar(TipoEventoAcceso.PAQUETE_ENTREGADO, ResultadoAcceso.INFORMATIVO,
                "Paquete entregado: " + p.getDescripcion(), p.getPropiedadId(), null, null, null,
                vigilanteId, null, p.getId());

        return toResponse(guardado);
    }

    public List<PaqueteResponse> listarPendientes() {
        return paqueteRepo.findAllByEstadoOrderByRecibidoEnDesc(EstadoPaquete.RECIBIDO)
                .stream().map(this::toResponse).toList();
    }

    public List<PaqueteResponse> listarPorPropiedad(Long propiedadId) {
        return paqueteRepo.findAllByPropiedadIdOrderByRecibidoEnDesc(propiedadId)
                .stream().map(this::toResponse).toList();
    }

    public long contarPendientes() {
        return paqueteRepo.countByEstado(EstadoPaquete.RECIBIDO);
    }

    // ── Residente ───────────────────────────────────────────────────────────

    public List<PaqueteResponse> listarMios(Long usuarioId) {
        List<Long> propiedadIds = usuarioPropiedadRepo.findByUsuarioId(usuarioId)
                .stream().map(UsuarioPropiedad::getPropiedadId).toList();
        if (propiedadIds.isEmpty()) return List.of();
        return paqueteRepo.findAllByPropiedadIdInOrderByRecibidoEnDesc(propiedadIds)
                .stream().map(this::toResponse).toList();
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void notificarResidentes(Long propiedadId, String ident) {
        try {
            List<Long> usuarioIds = usuarioPropiedadRepo.findByPropiedadId(propiedadId)
                    .stream().map(UsuarioPropiedad::getUsuarioId).distinct().toList();
            if (!usuarioIds.isEmpty()) {
                notificacionService.enviarAUsuarios(usuarioIds,
                        "Tienes un paquete",
                        "Llegó correspondencia a portería para " + ident,
                        Map.of("tipo", "PAQUETE", "propiedadId", String.valueOf(propiedadId)));
            }
        } catch (Exception ignored) {
            // best-effort
        }
    }

    private PaqueteResponse toResponse(Paquete p) {
        @SuppressWarnings("null")
		String ident = propiedadRepo.findById(p.getPropiedadId())
                .map(Propiedad::getPathCorto).orElse(null);
        return PaqueteResponse.from(p, ident);
    }
}
