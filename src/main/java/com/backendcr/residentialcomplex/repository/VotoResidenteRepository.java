package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.VotoResidente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface VotoResidenteRepository extends JpaRepository<VotoResidente, Long> {
    List<VotoResidente> findAllByVotacionId(Long votacionId);
    List<VotoResidente> findAllByVotacionIdAndResidenteId(Long votacionId, Long residenteId);
    boolean existsByVotacionIdAndResidenteId(Long votacionId, Long residenteId);
    void deleteAllByVotacionIdAndResidenteId(Long votacionId, Long residenteId);

    /** Cuenta cuántos residentes distintos han votado en una votación */
    @Query("SELECT COUNT(DISTINCT v.residenteId) FROM VotoResidente v WHERE v.votacionId = :votacionId")
    long countVotantesDistintos(Long votacionId);

    /** Cuenta votos por opción */
    @Query("SELECT v.opcionId, COUNT(v) FROM VotoResidente v WHERE v.votacionId = :votacionId GROUP BY v.opcionId")
    List<Object[]> contarPorOpcion(Long votacionId);

    /** Participantes únicos (residentes distintos) en un conjunto de votaciones */
    @Query("SELECT COUNT(DISTINCT v.residenteId) FROM VotoResidente v WHERE v.votacionId IN :ids")
    long countDistinctResidentesByVotacionIds(@Param("ids") Collection<Long> ids);
}
