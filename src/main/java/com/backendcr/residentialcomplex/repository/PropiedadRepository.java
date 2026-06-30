package com.backendcr.residentialcomplex.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.backendcr.residentialcomplex.entity.Propiedad;

public interface PropiedadRepository extends JpaRepository<Propiedad, Long> {

    Optional<Propiedad> findByTipoIdAndIdentificadorAndParentId(
            Long tipoId, String identificador, Long parentId);

    List<Propiedad> findByParentIdIsNull();

    List<Propiedad> findByParentId(Long parentId);

    @Query("Select p from Propiedad p join TipoPropiedad t on p.tipoId = t.id where t.esFacturable=true")
    List<Propiedad> findByTipoIdIsFacturable();

    @Query("Select COUNT(p) from Propiedad p join TipoPropiedad t on p.tipoId = t.id where t.esFacturable=true")
    Integer countPropiedadesIsFacturable();

    /**
     * Selector paginado de propiedades FACTURABLES con su path corto (concatenación
     * de identificadores desde la raíz, ej. "A101"), calculado en una sola consulta
     * con una CTE recursiva. Filtra por path o identificador y pagina en la BD.
     *
     * <p>La consulta corre sin prefijo de schema: el {@code search_path} del tenant
     * activo (multitenancy por schema) la resuelve a la base correcta.</p>
     *
     * <p>{@code :buscar} se espera no nulo (cadena vacía = sin filtro) para evitar
     * problemas de tipado de parámetros en la query nativa.</p>
     */
    @Query(value = """
            WITH RECURSIVE arbol AS (
                SELECT p.id, p.parent_id, p.tipo_id, p.identificador,
                       CAST(p.identificador AS text) AS path_corto
                FROM propiedades p
                WHERE p.parent_id IS NULL
                UNION ALL
                SELECT h.id, h.parent_id, h.tipo_id, h.identificador,
                       a.path_corto || h.identificador
                FROM propiedades h
                JOIN arbol a ON h.parent_id = a.id
            )
            SELECT a.id, a.identificador, a.path_corto, COUNT(*) OVER() AS total
            FROM arbol a
            JOIN tipos_propiedad t ON t.id = a.tipo_id
            WHERE t.es_facturable = true
              AND ( CAST(:buscar AS text) = ''
                    OR a.path_corto ILIKE '%' || CAST(:buscar AS text) || '%'
                    OR a.identificador ILIKE '%' || CAST(:buscar AS text) || '%' )
            ORDER BY a.path_corto
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> buscarFacturablesSelector(@Param("buscar") String buscar,
                                             @Param("size") int size,
                                             @Param("offset") int offset);
}
