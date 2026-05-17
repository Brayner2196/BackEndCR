package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pqr.PQRCambiarEstadoRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRHistorialResponse;
import com.backendcr.residentialcomplex.dto.pqr.PQRRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponderRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.PQRHistorial;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.repository.PQRHistorialRepository;
import com.backendcr.residentialcomplex.repository.PQRRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PQRService {

    private final PQRRepository pqrRepo;
    private final UsuarioRepository usuarioRepo;
    private final PQRHistorialRepository historialRepo;

    public List<PQRResponse> listarTodas() {
        return pqrRepo.findAll().stream().map(this::toResponse).toList();
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
                .stream().map(PQRHistorialResponse::from).toList();
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
        pqr.setFechaRespuesta(LocalDateTime.now());
        if (pqr.getEstado() == EstadoPQR.RADICADA || pqr.getEstado() == EstadoPQR.EN_PROCESO) {
            pqr.setEstado(EstadoPQR.RESUELTO);
        }
        PQR saved = pqrRepo.save(pqr);
        registrarHistorial(saved.getId(), estadoAnterior, saved.getEstado(), adminId, "Respondida por administrador");
        return toResponse(saved);
    }

    @Transactional
    public PQRResponse cambiarEstado(Long id, PQRCambiarEstadoRequest req, Long adminId) {
        PQR pqr = obtener(id);
        EstadoPQR estadoAnterior = pqr.getEstado();
        pqr.setEstado(req.estado());
        PQR saved = pqrRepo.save(pqr);
        registrarHistorial(saved.getId(), estadoAnterior, req.estado(), adminId, req.comentario());
        return toResponse(saved);
    }

    // ─── Helpers ──────────────────────────────────────────────

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
        return PQRResponse.from(p, nombre);
    }
}
