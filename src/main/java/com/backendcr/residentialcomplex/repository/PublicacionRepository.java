package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Publicacion;
import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    /** Todas las publicaciones activas, opcionalmente filtradas por categoría y búsqueda de texto. */
    @Query("""
        SELECT p FROM Publicacion p
        WHERE p.estado = 'ACTIVA'
          AND (:categoria IS NULL OR p.categoria = :categoria)
          AND (:busqueda IS NULL OR LOWER(p.titulo) LIKE LOWER(CONCAT('%', :busqueda, '%'))
               OR LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :busqueda, '%')))
        ORDER BY p.creadoEn DESC
        """)
    List<Publicacion> buscarActivas(
            @Param("categoria") CategoriaPublicacion categoria,
            @Param("busqueda") String busqueda);

    /** Publicaciones de un vendedor (todas excepto ELIMINADA). */
    List<Publicacion> findByVendedorIdAndEstadoNotOrderByCreadoEnDesc(
            Long vendedorId, EstadoPublicacion estado);

    /** Todas — para el admin. */
    List<Publicacion> findByEstadoNotOrderByCreadoEnDesc(EstadoPublicacion estado);
}
