package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pqr.PQRCambiarEstadoRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRHistorialResponse;
import com.backendcr.residentialcomplex.dto.pqr.PQRRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponderRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.PQRHistorial;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.repository.PQRHistorialRepository;
import com.backendcr.residentialcomplex.repository.PQRRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PQRService {

    private final PQRRepository pqrRepo;
    private final UsuarioRepository usuarioRepo;
    private final PQRHistorialRepository historialRepo;
    private final IdentidadRepository identidadRepo;
    private final PropiedadRepository propiedadRepo;
    private final NotificacionService notificacionService;

    public List<PQRResponse> listarTodas() {
        return pqrRepo.findAll(Sort.by(Sort.Direction.DESC, "creadoEn")).stream().map(this::toResponse).toList();
    }

    /** Alias para el ConsejoController — filtra por estado si se indica. */
    public List<PQRResponse> listarTodasParaConsejo(String estado) {
        if (estado != null && !estado.isBlank()) {
            try {
                EstadoPQR filtro = EstadoPQR.valueOf(estado.toUpperCase());
                return listarPorEstado(filtro);
            } catch (IllegalArgumentException ignored) { /* estado inválido → retorna todas */ }
        }
        return listarTodas();
    }

    public List<PQRResponse> listarPorEstado(EstadoPQR estado) {
        return pqrRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<PQRResponse> listarPorResidente(Long residenteId) {
        return pqrRepo.findAllByResidenteId(residenteId).stream().map(this::toResponse).toList();
    }

    public List<PQRHistorialResponse> listarHistorial(Long pqrId) {
        obtener(pqrId); // valida que exista
        return historialRepo.findAllByPqrIdOrderByFechaCambioAsc(pqrId)
                .stream().map(this::enriquecerHistorial).toList();
    }

    public List<PQRHistorialResponse> listarHistorialResidente(Long pqrId, Long residenteId) {
        PQR pqr = obtener(pqrId);
        if (!pqr.getResidenteId().equals(residenteId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes acceso a esta PQR");
        }
        return historialRepo.findAllByPqrIdOrderByFechaCambioAsc(pqrId)
                .stream().map(this::enriquecerHistorial).toList();
    }

    @Transactional
    public PQRResponse crear(PQRRequest req, Long residenteId) {
        PQR pqr = new PQR();
        pqr.setTipo(req.tipo());
        pqr.setAsunto(req.asunto());
        pqr.setDescripcion(req.descripcion());
        pqr.setPropiedadId(req.propiedadId());
        pqr.setResidenteId(residenteId);
        pqr.setEstado(EstadoPQR.RADICADA);
        PQR saved = pqrRepo.save(pqr);
        registrarHistorial(saved.getId(), null, EstadoPQR.RADICADA, residenteId, "PQR creada");
        notificarAdminsNuevaPqr(saved);
        return toResponse(saved);
    }

    @Transactional
    public PQRResponse responder(Long id, PQRResponderRequest req, Long adminId) {
        PQR pqr = obtener(id);
        if (pqr.getEstado() == EstadoPQR.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La PQR ya está cerrada");
        }
        EstadoPQR estadoAnterior = pqr.getEstado();
        pqr.setRespuestaAdmin(req.respuesta());
        pqr.setRespondidoPor(adminId);
        pqr.setFechaRespuesta(TenantClock.ahora());
        if (pqr.getEstado() == EstadoPQR.RADICADA || pqr.getEstado() == EstadoPQR.EN_PROCESO) {
            pqr.setEstado(EstadoPQR.RESUELTO);
        }
        PQR saved = pqrRepo.save(pqr);
        registrarHistorial(saved.getId(), estadoAnterior, saved.getEstado(), adminId, req.respuesta());
        notificarResidenteRespuesta(saved);
        return toResponse(saved);
    }

    @Transactional
    public PQRResponse cambiarEstado(Long id, PQRCambiarEstadoRequest req, Long adminId) {
        PQR pqr = obtener(id);
        EstadoPQR estadoAnterior = pqr.getEstado();
        pqr.setEstado(req.estado());
        PQR saved = pqrRepo.save(pqr);
        registrarHistorial(saved.getId(), estadoAnterior, req.estado(), adminId, req.comentario());
        notificarResidenteCambioEstado(saved);
        return toResponse(saved);
    }

    // ─── Notificaciones FCM ───────────────────────────────────

    /** Notifica a los admins del conjunto cuando un residente radica una PQR. */
    private void notificarAdminsNuevaPqr(PQR pqr) {
        String nombre = usuarioRepo.findById(pqr.getResidenteId())
                .map(Usuario::getNombre).orElse("Un residente");

        notificacionService.enviarAAdminsTenant(
                TenantContext.getTenant(),
                "📩 Nueva PQR radicada",
                String.format("%s radicó: \"%s\"", nombre, pqr.getAsunto()),
                Map.of(
                        "tipo",  "PQR_NUEVA",
                        "pqrId", pqr.getId().toString(),
                        "route", "pqr"
                )
        );
    }

    /** Notifica al residente cuando la administración responde su PQR. */
    private void notificarResidenteRespuesta(PQR pqr) {
        notificacionService.enviarAUsuario(
                pqr.getResidenteId(),
                "✅ Respuesta a tu PQR",
                String.format("La administración respondió tu PQR \"%s\".", pqr.getAsunto()),
                Map.of(
                        "tipo",   "PQR_RESPONDIDA",
                        "pqrId",  pqr.getId().toString(),
                        "estado", pqr.getEstado().name(),
                        "route",  "pqr"
                )
        );
    }

    /** Notifica al residente cuando la administración cambia el estado de su PQR. */
    private void notificarResidenteCambioEstado(PQR pqr) {
        notificacionService.enviarAUsuario(
                pqr.getResidenteId(),
                "🔄 Tu PQR cambió de estado",
                String.format("Tu PQR \"%s\" ahora está: %s.",
                        pqr.getAsunto(), estadoLegible(pqr.getEstado())),
                Map.of(
                        "tipo",   "PQR_ESTADO",
                        "pqrId",  pqr.getId().toString(),
                        "estado", pqr.getEstado().name(),
                        "route",  "pqr"
                )
        );
    }

    private String estadoLegible(EstadoPQR estado) {
        return switch (estado) {
            case RADICADA   -> "Radicada";
            case EN_PROCESO -> "En proceso";
            case RESUELTO   -> "Resuelta";
            case CERRADO    -> "Cerrada";
            case RECHAZADA  -> "Rechazada";
        };
    }

    // ─── Helpers ──────────────────────────────────────────────

    private PQRHistorialResponse enriquecerHistorial(PQRHistorial h) {
        String nombre = "Sistema";
        String rol = "";
        if (h.getCambiadoPor() != null) {
            Optional<com.backendcr.residentialcomplex.entity.Usuario> usuOpt =
                    usuarioRepo.findById(h.getCambiadoPor());
            if (usuOpt.isPresent()) {
                var usu = usuOpt.get();
                nombre = usu.getNombre();
                rol = identidadRepo.findById(usu.getIdentidadId())
                        .map(id -> id.getRol())
                        .orElse("");
            }
        }
        return PQRHistorialResponse.from(h, nombre, rol);
    }

    private void registrarHistorial(Long pqrId, EstadoPQR anterior, EstadoPQR nuevo,
                                    Long cambiadoPor, String comentario) {
        historialRepo.save(new PQRHistorial(pqrId, anterior, nuevo, cambiadoPor, comentario));
    }

    private PQR obtener(Long id) {
        return pqrRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PQR no encontrada"));
    }

    private PQRResponse toResponse(PQR p) {
        String nombre = usuarioRepo.findById(p.getResidenteId())
                .map(Usuario::getNombre).orElse("N/A");
        String propiedadIdentificador = p.getPropiedadId() != null
                ? propiedadRepo.findById(p.getPropiedadId())
                        .map(Propiedad::getIdentificador).orElse(null)
                : null;
        return PQRResponse.from(p, nombre, propiedadIdentificador);
    }
}
