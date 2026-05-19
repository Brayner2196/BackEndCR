package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.anuncio.*;
import com.backendcr.residentialcomplex.entity.Anuncio;
import com.backendcr.residentialcomplex.entity.AnuncioVista;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import com.backendcr.residentialcomplex.repository.AnuncioRepository;
import com.backendcr.residentialcomplex.repository.AnuncioVistaRepository;
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
public class AnuncioService {

    private final AnuncioRepository anuncioRepo;
    private final AnuncioVistaRepository vistaRepo;
    private final UsuarioRepository usuarioRepo;
    private final NotificacionService notificacionService;

    // ─── Admin ──────────────────────────────────────────────────────────────

    public List<AnuncioResponse> listarTodos(EstadoAnuncio estado) {
        List<Anuncio> lista = estado != null
                ? anuncioRepo.findAllByEstadoOrderByCreadoEnDesc(estado)
                : anuncioRepo.findAllByOrderByCreadoEnDesc();
        return lista.stream().map(a -> toResponse(a, null, false)).toList();
    }

    @Transactional
    public AnuncioResponse crear(AnuncioRequest req, Long adminId) {
        Anuncio a = new Anuncio();
        a.setTitulo(req.titulo());
        a.setContenido(req.contenido());
        a.setCreadoPor(adminId);
        a.setEstado(EstadoAnuncio.ACTIVO);
        if (req.fechaInicio() != null) a.setFechaInicio(LocalDateTime.parse(req.fechaInicio()));
        if (req.fechaFin() != null) a.setFechaFin(LocalDateTime.parse(req.fechaFin()));
        AnuncioResponse response = toResponse(anuncioRepo.save(a), null, false);

        notificacionService.enviarATenant(
            TenantContext.getTenant(),
            "📢 Nuevo anuncio",
            req.titulo(),
            java.util.Map.of("tipo", "ANUNCIO", "anuncioId", String.valueOf(response.id()), "route", "anuncios")
        );

        return response;
    }

    @Transactional
    public AnuncioResponse actualizar(Long id, AnuncioRequest req) {
        Anuncio a = obtener(id);
        a.setTitulo(req.titulo());
        a.setContenido(req.contenido());
        if (req.fechaInicio() != null) a.setFechaInicio(LocalDateTime.parse(req.fechaInicio()));
        else a.setFechaInicio(null);
        if (req.fechaFin() != null) a.setFechaFin(LocalDateTime.parse(req.fechaFin()));
        else a.setFechaFin(null);
        return toResponse(anuncioRepo.save(a), null, false);
    }

    @Transactional
    public AnuncioResponse cambiarEstado(Long id, CambiarEstadoAnuncioRequest req) {
        Anuncio a = obtener(id);
        a.setEstado(req.estado());
        return toResponse(anuncioRepo.save(a), null, false);
    }

    @Transactional
    public void eliminar(Long id) {
        obtener(id); // valida existencia
        vistaRepo.findAllByAnuncioId(id).forEach(v -> vistaRepo.deleteById(v.getId()));
        anuncioRepo.deleteById(id);
    }

    public List<AnuncioVistaResponse> listarVistas(Long anuncioId) {
        obtener(anuncioId);
        return vistaRepo.findAllByAnuncioId(anuncioId)
                .stream().map(AnuncioVistaResponse::from).toList();
    }

    // ─── Residente ──────────────────────────────────────────────────────────

    /** Lista anuncios ACTIVOS para el residente; incluye si ya los vio */
    public List<AnuncioResponse> listarParaResidente(Long residenteId) {
        return anuncioRepo.findAllByEstadoOrderByCreadoEnDesc(EstadoAnuncio.ACTIVO)
                .stream()
                .map(a -> toResponse(a, residenteId, vistaRepo.existsByAnuncioIdAndResidenteId(a.getId(), residenteId)))
                .toList();
    }

    /** Registra la vista del residente (idempotente) y retorna el anuncio */
    @Transactional
    public AnuncioResponse marcarVisto(Long anuncioId, Long residenteId) {
        Anuncio a = obtener(anuncioId);
        if (a.getEstado() != EstadoAnuncio.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.GONE, "Anuncio no disponible");
        }
        if (!vistaRepo.existsByAnuncioIdAndResidenteId(anuncioId, residenteId)) {
            String nombre = usuarioRepo.findById(residenteId).map(u -> u.getNombre()).orElse("N/A");
            AnuncioVista v = new AnuncioVista();
            v.setAnuncioId(anuncioId);
            v.setResidenteId(residenteId);
            v.setResidenteNombre(nombre);
            vistaRepo.save(v);
        }
        return toResponse(a, residenteId, true);
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Anuncio obtener(Long id) {
        return anuncioRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Anuncio no encontrado"));
    }

    private AnuncioResponse toResponse(Anuncio a, Long residenteId, boolean vistoPorMi) {
        String nombre = usuarioRepo.findById(a.getCreadoPor()).map(u -> u.getNombre()).orElse("N/A");
        long vistas = vistaRepo.countByAnuncioId(a.getId());
        return AnuncioResponse.from(a, nombre, vistas, vistoPorMi);
    }
}
