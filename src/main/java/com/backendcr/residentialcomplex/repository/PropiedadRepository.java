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
     * Selector paginado de propiedades FACTURABLES con su path corto (columna
     * denormalizada {@code path_corto}, ej. "A101"). Filtra por path o
     * identificador y pagina en la BD.
     *
     * <p>Antes usaba una CTE recursiva; ahora lee la columna persistida, que se
     * mantiene sincronizada al crear/renombrar/mover propiedades
     * (ver {@link com.backendcr.residentialcomplex.service.PropiedadPathCalculator}).
     * El filtro por substring aprovecha el índice GIN {@code pg_trgm}
     * {@code idx_prop_path_corto_trgm}.</p>
     *
     * <p>La consulta corre sin prefijo de schema: el {@code search_path} del tenant
     * activo (multitenancy por schema) la resuelve a la base correcta.</p>
     *
     * <p>{@code :buscar} se espera no nulo (cadena vacía = sin filtro) para evitar
     * problemas de tipado de parámetros en la query nativa.</p>
     */
    @Query(value = """
            SELECT p.id, p.identificador, p.path_corto, COUNT(*) OVER() AS total
            FROM propiedades p
            JOIN tipos_propiedad t ON t.id = p.tipo_id
            WHERE t.es_facturable = true
              AND ( CAST(:buscar AS text) = ''
                    OR p.path_corto ILIKE '%' || CAST(:buscar AS text) || '%'
                    OR p.identificador ILIKE '%' || CAST(:buscar AS text) || '%' )
            ORDER BY p.path_corto
            LIMIT :size OFFSET :offset
            """, nativeQuery = true)
    List<Object[]> buscarFacturablesSelector(@Param("buscar") String buscar,
                                             @Param("size") int size,
                                             @Param("offset") int offset);
}
