package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.votacion.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import com.backendcr.residentialcomplex.entity.enums.TipoVotacion;
import com.backendcr.residentialcomplex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VotacionService {

    private final VotacionRepository votacionRepo;
    private final OpcionVotacionRepository opcionRepo;
    private final VotoResidenteRepository votoRepo;
    private final UsuarioRepository usuarioRepo;

    // ─── Admin: CRUD ─────────────────────────────────────────────────────────

    public List<VotacionResponse> listarTodas(EstadoVotacion estado) {
        List<Votacion> lista = estado != null
                ? votacionRepo.findAllByEstadoOrderByCreadoEnDesc(estado)
                : votacionRepo.findAllByOrderByCreadoEnDesc();
        return lista.stream().map(v -> toResponse(v, null, true)).toList();
    }

    public VotacionResponse obtenerDetalle(Long id) {
        return toResponse(obtener(id), null, true);
    }

    @Transactional
    public VotacionResponse crear(VotacionRequest req, Long adminId) {
        Votacion v = new Votacion();
        aplicarCampos(v, req);
        v.setCreadoPor(adminId);
        v.setEstado(EstadoVotacion.BORRADOR);
        Votacion saved = votacionRepo.save(v);
        guardarOpciones(saved.getId(), req.opciones());
        return toResponse(saved, null, true);
    }

    @Transactional
    public VotacionResponse actualizar(Long id, VotacionRequest req) {
        Votacion v = obtener(id);
        if (v.getEstado() == EstadoVotacion.CERRADA || v.getEstado() == EstadoVotacion.ARCHIVADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede editar una votación cerrada");
        }
        aplicarCampos(v, req);
        opcionRepo.deleteAllByVotacionId(id);
        guardarOpciones(id, req.opciones());
        return toResponse(votacionRepo.save(v), null, true);
    }

    @Transactional
    public VotacionResponse cambiarEstado(Long id, CambiarEstadoVotacionRequest req) {
        Votacion v = obtener(id);
        v.setEstado(req.estado());
        return toResponse(votacionRepo.save(v), null, true);
    }

    /** Resultados detallados para el admin */
    public VotacionResponse resultadosAdmin(Long id) {
        return toResponse(obtener(id), null, true);
    }

    // ─── Residente ───────────────────────────────────────────────────────────

    public List<VotacionResponse> listarParaResidente(Long residenteId) {
        return votacionRepo.findAllByEstadoOrderByCreadoEnDesc(EstadoVotacion.ABIERTA)
                .stream()
                .map(v -> toResponse(v, residenteId, false))
                .toList();
    }

    public VotacionResponse detalleParaResidente(Long votacionId, Long residenteId) {
        Votacion v = obtener(votacionId);
        if (v.getEstado() != EstadoVotacion.ABIERTA) {
            throw new ResponseStatusException(HttpStatus.GONE, "Votación no disponible");
        }
        return toResponse(v, residenteId, false);
    }

    @Transactional
    public VotacionResponse votar(Long votacionId, Long residenteId, RegistrarVotoRequest req) {
        Votacion v = obtener(votacionId);
        if (v.getEstado() != EstadoVotacion.ABIERTA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La votación no está abierta");
        }
        boolean yaVoto = votoRepo.existsByVotacionIdAndResidenteId(votacionId, residenteId);
        if (yaVoto && !v.isPermiteCambiarVoto()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya registraste tu voto y no se permite cambiarlo");
        }
        // Si ya votó y se permite cambio, borramos los votos anteriores
        if (yaVoto) {
            votoRepo.deleteAllByVotacionIdAndResidenteId(votacionId, residenteId);
        }

        String nombre = usuarioRepo.findById(residenteId).map(u -> u.getNombre()).orElse("N/A");

        switch (v.getTipoVotacion()) {
            case OPCION_UNICA -> {
                if (req.opcionIds() == null || req.opcionIds().size() != 1) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes seleccionar exactamente una opción");
                }
                guardarVotoOpcion(votacionId, residenteId, nombre, req.opcionIds().get(0));
            }
            case OPCION_MULTIPLE -> {
                if (req.opcionIds() == null || req.opcionIds().isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes seleccionar al menos una opción");
                }
                req.opcionIds().forEach(opId -> guardarVotoOpcion(votacionId, residenteId, nombre, opId));
            }
            case ESCALA_NUMERICA -> {
                if (req.valorNumerico() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes proporcionar un valor numérico");
                }
                int max = v.getEscalaMax() != null ? v.getEscalaMax() : 5;
                if (req.valorNumerico() < 1 || req.valorNumerico() > max) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valor fuera del rango permitido (1-" + max + ")");
                }
                VotoResidente voto = new VotoResidente();
                voto.setVotacionId(votacionId);
                voto.setResidenteId(residenteId);
                voto.setResidenteNombre(nombre);
                voto.setValorNumerico(req.valorNumerico());
                votoRepo.save(voto);
            }
            case TEXTO_LIBRE -> {
                if (req.respuestaTexto() == null || req.respuestaTexto().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes proporcionar una respuesta");
                }
                VotoResidente voto = new VotoResidente();
                voto.setVotacionId(votacionId);
                voto.setResidenteId(residenteId);
                voto.setResidenteNombre(nombre);
                voto.setRespuestaTexto(req.respuestaTexto());
                votoRepo.save(voto);
            }
        }
        return toResponse(v, residenteId, false);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void aplicarCampos(Votacion v, VotacionRequest req) {
        v.setTitulo(req.titulo());
        v.setDescripcion(req.descripcion());
        v.setTipoVotacion(req.tipoVotacion());
        v.setEscalaMax(req.escalaMax());
        v.setMostrarVotantes(req.mostrarVotantes());
        v.setPermiteCambiarVoto(req.permiteCambiarVoto());
        if (req.fechaInicio() != null) v.setFechaInicio(LocalDateTime.parse(req.fechaInicio()));
        else v.setFechaInicio(null);
        if (req.fechaFin() != null) v.setFechaFin(LocalDateTime.parse(req.fechaFin()));
        else v.setFechaFin(null);
    }

    private void guardarOpciones(Long votacionId, List<String> opciones) {
        if (opciones == null) return;
        for (int i = 0; i < opciones.size(); i++) {
            OpcionVotacion op = new OpcionVotacion();
            op.setVotacionId(votacionId);
            op.setTexto(opciones.get(i));
            op.setOrden(i);
            opcionRepo.save(op);
        }
    }

    private void guardarVotoOpcion(Long votacionId, Long residenteId, String nombre, Long opcionId) {
        VotoResidente voto = new VotoResidente();
        voto.setVotacionId(votacionId);
        voto.setResidenteId(residenteId);
        voto.setResidenteNombre(nombre);
        voto.setOpcionId(opcionId);
        votoRepo.save(voto);
    }

    private Votacion obtener(Long id) {
        return votacionRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Votación no encontrada"));
    }

    private VotacionResponse toResponse(Votacion v, Long residenteId, boolean esAdmin) {
        List<OpcionVotacion> opciones = opcionRepo.findAllByVotacionIdOrderByOrden(v.getId());

        // Construir mapa opcionId -> totalVotos
        Map<Long, Long> conteoOpciones = new HashMap<>();
        votoRepo.contarPorOpcion(v.getId()).forEach(row -> {
            Long opId = (Long) row[0];
            Long count = (Long) row[1];
            if (opId != null) conteoOpciones.put(opId, count);
        });

        List<OpcionVotacionResponse> opcionesResp = opciones.stream()
                .map(op -> OpcionVotacionResponse.from(op, conteoOpciones.getOrDefault(op.getId(), 0L)))
                .toList();

        long totalVotantes = votoRepo.countVotantesDistintos(v.getId());
        boolean yaVote = residenteId != null && votoRepo.existsByVotacionIdAndResidenteId(v.getId(), residenteId);

        // Mostrar votantes: admin siempre, residente solo si mostrarVotantes=true
        List<VotoResidenteResponse> votantes = null;
        if (esAdmin || v.isMostrarVotantes()) {
            // Agrupar por residenteId para no duplicar (importante en OPCION_MULTIPLE)
            Map<Long, List<VotoResidente>> porResidente = votoRepo.findAllByVotacionId(v.getId())
                    .stream().collect(Collectors.groupingBy(VotoResidente::getResidenteId));
            votantes = porResidente.values().stream()
                    .map(votos -> VotoResidenteResponse.from(votos.get(0)))
                    .toList();
        }

        return VotacionResponse.from(v, totalVotantes, yaVote, opcionesResp, votantes);
    }
}
