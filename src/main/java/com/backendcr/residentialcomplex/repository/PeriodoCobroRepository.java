package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.PeriodoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PeriodoCobroRepository extends JpaRepository<PeriodoCobro, Long> {
    Optional<PeriodoCobro> findByEstado(EstadoPeriodo estado);
    boolean existsByAnioAndMes(int anio, int mes);
    List<PeriodoCobro> findAllByOrderByAnioDescMesDesc();
    Optional<PeriodoCobro> findByAnioAndMes(int anio, int mes);
}
