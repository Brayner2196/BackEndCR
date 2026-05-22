package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.HorarioGrupoZona;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HorarioGrupoZonaRepository extends JpaRepository<HorarioGrupoZona, Long> {
    List<HorarioGrupoZona> findByZonaComunIdOrderByOrdenAsc(Long zonaComunId);
    void deleteByZonaComunId(Long zonaComunId);
}
