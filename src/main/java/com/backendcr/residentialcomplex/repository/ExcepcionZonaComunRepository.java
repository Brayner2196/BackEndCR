package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ExcepcionZonaComun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExcepcionZonaComunRepository extends JpaRepository<ExcepcionZonaComun, Long> {
    List<ExcepcionZonaComun> findAllByZonaComunIdOrderByFechaAsc(Long zonaComunId);
    Optional<ExcepcionZonaComun> findByZonaComunIdAndFecha(Long zonaComunId, LocalDate fecha);
    void deleteByZonaComunIdAndId(Long zonaComunId, Long id);
}
