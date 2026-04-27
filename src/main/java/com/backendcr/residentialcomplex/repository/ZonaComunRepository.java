package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ZonaComun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZonaComunRepository extends JpaRepository<ZonaComun, Long> {
    List<ZonaComun> findAllByActivaTrue();
}
