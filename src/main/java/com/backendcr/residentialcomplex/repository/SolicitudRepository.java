package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Solicitud;
import com.backendcr.residentialcomplex.entity.enums.EstadoSolicitud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    /** Solicitudes enviadas por el comprador, más recientes primero. */
    List<Solicitud> findByCompradorIdOrderByCreadoEnDesc(Long compradorId);

    /** Solicitudes recibidas por el vendedor, más recientes primero. */
    List<Solicitud> findByVendedorIdOrderByCreadoEnDesc(Long vendedorId);

    /** Solicitudes de una publicación específica (para validaciones de stock). */
    long countByPublicacionIdAndEstado(Long publicacionId, EstadoSolicitud estado);
}
