package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.MovimientoAbono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface MovimientoAbonoRepository extends JpaRepository<MovimientoAbono, Long> {
    List<MovimientoAbono> findAllByAbonoId(Long abonoId);
    List<MovimientoAbono> findAllByCobroId(Long cobroId);
    boolean existsByCobroId(Long cobroId);

    /** Devuelve los cobroIds (del conjunto dado) que tienen al menos un MovimientoAbono. */
    @Query("SELECT DISTINCT m.cobroId FROM MovimientoAbono m WHERE m.cobroId IN :cobroIds AND m.cobroId IS NOT NULL")
    Set<Long> findCobroIdsWithMovimientos(@Param("cobroIds") Collection<Long> cobroIds);
}
