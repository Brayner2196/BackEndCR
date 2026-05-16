package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Publicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PublicacionRepository extends JpaRepository<Publicacion, Long> {

    /**
     * Publicaciones ACTIVAS con búsqueda de texto (ILIKE, case-insensitive) y filtro de categoría.
     * Usa native query para evitar problemas de tipado de parámetros nulos en PostgreSQL.
     */
    @Query(value = """
        SELECT * FROM publicaciones
        WHERE estado = 'ACTIVA'
          AND (CAST(:categoria AS text) IS NULL OR categoria = CAST(:categoria AS text))
          AND (CAST(:busqueda AS text) IS NULL
               OR titulo ILIKE '%' || CAST(:busqueda AS text) || '%'
               OR descripcion ILIKE '%' || CAST(:busqueda AS text) || '%')
        ORDER BY creado_en DESC
        """, nativeQuery = true)
    List<Publicacion> buscarActivas(
            @Param("categoria") String categoria,
            @Param("busqueda") String busqueda);

    /** Publicaciones de un vendedor (todas excepto ELIMINADA). */
    List<Publicacion> findByVendedorIdAndEstadoNotOrderByCreadoEnDesc(
            Long vendedorId, EstadoPublicacion estado);

    /** Todas — para el admin. */
    List<Publicacion> findByEstadoNotOrderByCreadoEnDesc(EstadoPublicacion estado);
}
