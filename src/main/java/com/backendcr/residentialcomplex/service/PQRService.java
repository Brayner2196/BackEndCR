package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pqr.PQRCambiarEstadoRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponderRequest;
import com.backendcr.residentialcomplex.dto.pqr.PQRResponse;
import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
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

    public List<PQRResponse> listarTodas() {
        return pqrRepo.findAll().stream().map(this::toResponse).toList();
    }

    public List<PQRResponse> listarPorEstado(EstadoPQR estado) {
        return pqrRepo.findAllByEstado(estado).stream().map(this::toResponse).toList();
    }

    public List<PQRResponse> listarPorResidente(Long residenteId) {
        return pqrRepo.findAllByResidenteId(residenteId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PQRResponse crear(PQRRequest req, Long residenteId) {
        PQR pqr = new PQR();
        pqr.setTipo(req.tipo());
        pqr.setAsunto(req.asunto());
        pqr.setDescripcion(req.descripcion());
        pqr.setPropiedadId(req.propiedadId());
        pqr.setResidenteId(residenteId);
        pqr.setEstado(EstadoPQR.PENDIENTE);
        return toResponse(pqrRepo.save(pqr));
    }

    @Transactional
    public PQRResponse responder(Long id, PQRResponderRequest req, Long adminId) {
        PQR pqr = obtener(id);
        if (pqr.getEstado() == EstadoPQR.CERRADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La PQR ya está cerrada");
        }
        pqr.setRespuestaAdmin(req.respuesta());
        pqr.setRespondidoPor(adminId);
        pqr.setFechaRespuesta(LocalDateTime.now());
        if (pqr.getEstado() == EstadoPQR.PENDIENTE) {
            pqr.setEstado(EstadoPQR.RESUELTO);
        }
        return toResponse(pqrRepo.save(pqr));
    }

    @Transactional
    public PQRResponse cambiarEstado(Long id, PQRCambiarEstadoRequest req) {
        PQR pqr = obtener(id);
        pqr.setEstado(req.estado());
        return toResponse(pqrRepo.save(pqr));
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
