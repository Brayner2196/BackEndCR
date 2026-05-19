package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.solicitud.ActualizarEstadoSolicitudRequest;
import com.backendcr.residentialcomplex.dto.solicitud.SolicitudRequest;
import com.backendcr.residentialcomplex.dto.solicitud.SolicitudResponse;
import com.backendcr.residentialcomplex.entity.Publicacion;
import com.backendcr.residentialcomplex.entity.Solicitud;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoSolicitud;
import com.backendcr.residentialcomplex.entity.enums.TipoSolicitud;
import com.backendcr.residentialcomplex.repository.PublicacionRepository;
import com.backendcr.residentialcomplex.repository.SolicitudRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SolicitudService {

    private final SolicitudRepository solicitudRepo;
    private final PublicacionRepository publicacionRepo;
    private final UsuarioRepository usuarioRepo;
    private final NotificacionService notificacionService;

    // ─── Crear solicitud (comprador) ──────────────────────────

    @Transactional
    public SolicitudResponse crear(Long compradorId, SolicitudRequest req) {
        Publicacion pub = publicacionRepo.findById(req.publicacionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Publicación no encontrada"));

        // Validaciones de negocio
        if (pub.getEstado() != EstadoPublicacion.ACTIVA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Esta publicación no está disponible");
        }
        if (pub.getVendedorId().equals(compradorId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No puedes solicitar tu propio producto");
        }
        if (req.tipo() == TipoSolicitud.DOMICILIO && !pub.isAceptaDomicilio()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Este vendedor no ofrece domicilio para este producto");
        }
        if (pub.getStock() != null && pub.getStock() <= 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto está agotado");
        }
        if (pub.getStock() != null && req.cantidad() > pub.getStock()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente. Disponibles: " + pub.getStock());
        }

        Usuario comprador = usuarioRepo.findById(compradorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Comprador no encontrado"));

        Solicitud solicitud = new Solicitud();
        solicitud.setPublicacionId(pub.getId());
        solicitud.setPublicacionTitulo(pub.getTitulo());
        solicitud.setPublicacionPrecio(pub.getPrecio());
        solicitud.setCompradorId(compradorId);
        solicitud.setCompradorNombre(comprador.getNombre());
        solicitud.setVendedorId(pub.getVendedorId());
        solicitud.setVendedorNombre(pub.getVendedorNombre());
        solicitud.setTipo(req.tipo());
        solicitud.setCantidad(req.cantidad());
        solicitud.setNotas(req.notas());
        solicitud.setEstado(EstadoSolicitud.PENDIENTE);

        Solicitud guardada = solicitudRepo.save(solicitud);

        // ── Notificación FCM al vendedor ───────────────────────
        notificarVendedor(guardada);

        return SolicitudResponse.from(guardada);
    }

    // ─── Solicitudes enviadas (comprador) ──────────────────────

    public List<SolicitudResponse> misSolicitudesEnviadas(Long compradorId) {
        return solicitudRepo.findByCompradorIdOrderByCreadoEnDesc(compradorId)
                .stream().map(SolicitudResponse::from).toList();
    }

    // ─── Solicitudes recibidas (vendedor) ──────────────────────

    public List<SolicitudResponse> misSolicitudesRecibidas(Long vendedorId) {
        return solicitudRepo.findByVendedorIdOrderByCreadoEnDesc(vendedorId)
                .stream().map(SolicitudResponse::from).toList();
    }

    // ─── Actualizar estado (vendedor acepta/rechaza) ────────────

    @Transactional
    public SolicitudResponse actualizarEstado(Long solicitudId, Long usuarioId,
                                               ActualizarEstadoSolicitudRequest req) {
        Solicitud s = solicitudRepo.findById(solicitudId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Solicitud no encontrada"));

        // Solo el vendedor puede aceptar/rechazar; solo el comprador puede cancelar
        boolean esVendedor  = s.getVendedorId().equals(usuarioId);
        boolean esComprador = s.getCompradorId().equals(usuarioId);
        EstadoSolicitud nuevoEstado = req.estado();

        if (!esVendedor && !esComprador) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso sobre esta solicitud");
        }
        if (nuevoEstado == EstadoSolicitud.CANCELADA && !esComprador) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el comprador puede cancelar una solicitud");
        }
        if ((nuevoEstado == EstadoSolicitud.ACEPTADA ||
             nuevoEstado == EstadoSolicitud.RECHAZADA) && !esVendedor) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo el vendedor puede aceptar o rechazar");
        }
        if (s.getEstado() != EstadoSolicitud.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La solicitud ya fue procesada: " + s.getEstado().name());
        }

        s.setEstado(nuevoEstado);
        Solicitud actualizada = solicitudRepo.save(s);

        // Notificar al comprador del resultado
        notificarComprador(actualizada);

        return SolicitudResponse.from(actualizada);
    }

    // ─── Notificaciones FCM ────────────────────────────────────

    private void notificarVendedor(Solicitud s) {
        String tipoTexto = s.getTipo() == TipoSolicitud.DOMICILIO
                ? "a domicilio" : "para recogida";

        String cuerpo = String.format(
                "%s solicita %d × \"%s\" %s.",
                s.getCompradorNombre(),
                s.getCantidad(),
                s.getPublicacionTitulo(),
                tipoTexto
        );
        if (s.getNotas() != null && !s.getNotas().isBlank()) {
            cuerpo += " Nota: " + s.getNotas();
        }

        notificacionService.enviarAUsuario(
                s.getVendedorId(),
                "📦 Nueva solicitud de pedido",
                cuerpo,
                Map.of(
                        "tipo",         "SOLICITUD_RECIBIDA",
                        "solicitudId",  s.getId().toString(),
                        "compradorId",  s.getCompradorId().toString(),
                        "route",        "marketplace"
                )
        );
    }

    private void notificarComprador(Solicitud s) {
        String titulo;
        String cuerpo;

        if (s.getEstado() == EstadoSolicitud.ACEPTADA) {
            titulo = "✅ Solicitud aceptada";
            cuerpo = String.format(
                    "%s aceptó tu pedido de \"%s\". Coordina los detalles con el vendedor.",
                    s.getVendedorNombre(),
                    s.getPublicacionTitulo()
            );
        } else {
            titulo = "❌ Solicitud rechazada";
            cuerpo = String.format(
                    "%s no pudo atender tu pedido de \"%s\".",
                    s.getVendedorNombre(),
                    s.getPublicacionTitulo()
            );
        }

        notificacionService.enviarAUsuario(
                s.getCompradorId(),
                titulo,
                cuerpo,
                Map.of(
                        "tipo",         "SOLICITUD_ACTUALIZADA",
                        "solicitudId",  s.getId().toString(),
                        "estado",       s.getEstado().name(),
                        "route",        "marketplace"
                )
        );
    }
}
