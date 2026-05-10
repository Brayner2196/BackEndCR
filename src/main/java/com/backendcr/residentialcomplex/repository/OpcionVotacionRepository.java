package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.OpcionVotacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionVotacionRepository extends JpaRepository<OpcionVotacion, Long> {
    List<OpcionVotacion> findAllByVotacionIdOrderByOrden(Long votacionId);
    void deleteAllByVotacionId(Long votacionId);
}
