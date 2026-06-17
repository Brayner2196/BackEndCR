package com.backendcr.residentialcomplex.service.cartera;

import com.backendcr.residentialcomplex.dto.cartera.AvisoCobranzaResponse;
import com.backendcr.residentialcomplex.dto.cartera.NotificarCarteraRequest;
import com.backendcr.residentialcomplex.entity.AvisoCobranza;
import com.backendcr.residentialcomplex.entity.EstadoCartera;
import com.backendcr.residentialcomplex.entity.EstadoCarteraPropiedad;
import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;
import com.backendcr.residentialcomplex.repository.AvisoCobranzaRepository;
import com.backendcr.residentialcomplex.repository.EstadoCarteraPropiedadRepository;
import com.backendcr.residentialcomplex.repository.EstadoCarteraRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.service.NotificacionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Gestión de cobranza sobre morosos: envío de avisos de fase de cartera
 * (recordatorio, paso a cartera, pre-jurídico, etc.) y su auditoría.
 *
 * Reutiliza {@link NotificacionService} para el push y registra cada envío
 * en {@link AvisoCobranza} para trazabilidad. Lógica separada y reutilizable:
 * el controller solo orquesta.
 */
@Service
@RequiredArgsConstructor
public class GestionCarteraService {

    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final EstadoCarteraRepository estadoRepo;
    private final EstadoCarteraPropiedadRepository snapshotRepo;
    private final AvisoCobranzaRepository avisoRepo;
    private final NotificacionService notificacionService;

    /** Notifica el aviso a todos los residentes de una propiedad. */
    @Transactional
    public AvisoCobranzaResponse notificarPropiedad(
            Long propiedadId, NotificarCarteraRequest req, String enviadoPor) {

        final NotificarCarteraRequest datos =
                req != null ? req : new NotificarCarteraRequest(null, null);

        final EstadoCartera fase = resolverFase(propiedadId, datos.estadoCarteraId());
        final String titulo = construirTitulo(fase);
        final String cuerpo = construirCuerpo(fase, datos.mensaje());

        final List<Long> usuarios = usuarioPropiedadRepo.findByPropiedadId(propiedadId)
                .stream()
                .map(UsuarioPropiedad::getUsuarioId)
                .distinct()
                .toList();

        if (!usuarios.isEmpty()) {
            notificacionService.enviarAUsuarios(
                    usuarios,
                    titulo,
                    cuerpo,
                    Map.of(
                            "tipo", "COBRANZA",
                            "route", "pagos",
                            "propiedadId", String.valueOf(propiedadId)
                    )
            );
        }

        registrarAviso(propiedadId, fase, titulo, cuerpo, usuarios.size(), enviadoPor);

        return new AvisoCobranzaResponse(
                propiedadId,
                fase != null ? fase.getNombre() : null,
                usuarios.size(),
                !usuarios.isEmpty()
        );
    }

    /**
     * Notifica de forma masiva a todas las propiedades cuya fase de cartera
     * vigente es la indicada (caso típico: "avisar a todos los de pre-jurídico").
     */
    @Transactional
    public List<AvisoCobranzaResponse> notificarMasivoPorEstado(
            Long estadoCarteraId, String mensaje, String enviadoPor) {

        if (estadoCarteraId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Se requiere la fase de cartera para el envío masivo");
        }

        final List<EstadoCarteraPropiedad> propiedades =
                snapshotRepo.findByEstadoCarteraId(estadoCarteraId);

        final List<AvisoCobranzaResponse> resultados = new ArrayList<>();
        for (final EstadoCarteraPropiedad p : propiedades) {
            resultados.add(notificarPropiedad(
                    p.getPropiedadId(),
                    new NotificarCarteraRequest(estadoCarteraId, mensaje),
                    enviadoPor));
        }
        return resultados;
    }

    // ─────────────────────────────────────────────────────────────
    // Helpers privados (resolución de fase, textos y auditoría)
    // ─────────────────────────────────────────────────────────────

    /** Si llega una fase explícita la valida; si no, usa la vigente de la propiedad. */
    private EstadoCartera resolverFase(Long propiedadId, Long estadoCarteraId) {
        if (estadoCarteraId != null) {
            return estadoRepo.findById(estadoCarteraId).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Fase de cartera no encontrada"));
        }
        return snapshotRepo.findByPropiedadId(propiedadId)
                .flatMap(s -> estadoRepo.findById(s.getEstadoCarteraId()))
                .orElse(null);
    }

    private String construirTitulo(EstadoCartera fase) {
        return fase != null
                ? "Aviso de cartera: " + fase.getNombre()
                : "Aviso de cartera";
    }

    private String construirCuerpo(EstadoCartera fase, String mensaje) {
        if (mensaje != null && !mensaje.isBlank()) {
            return mensaje.trim();
        }
        if (fase != null) {
            final String base = "Tu propiedad se encuentra en estado de cartera \""
                    + fase.getNombre() + "\". Regulariza tu saldo para evitar avanzar de fase.";
            return fase.getDescripcion() != null && !fase.getDescripcion().isBlank()
                    ? base + " " + fase.getDescripcion().trim()
                    : base;
        }
        return "Tienes saldos pendientes. Por favor regulariza tu cartera lo antes posible.";
    }

    private void registrarAviso(Long propiedadId, EstadoCartera fase, String titulo,
                                String cuerpo, int notificados, String enviadoPor) {
        final AvisoCobranza aviso = new AvisoCobranza();
        aviso.setPropiedadId(propiedadId);
        aviso.setEstadoCarteraId(fase != null ? fase.getId() : null);
        aviso.setTitulo(titulo);
        aviso.setMensaje(cuerpo.length() > 500 ? cuerpo.substring(0, 500) : cuerpo);
        aviso.setUsuariosNotificados(notificados);
        aviso.setEnviadoPor(enviadoPor);
        avisoRepo.save(aviso);
    }
}
