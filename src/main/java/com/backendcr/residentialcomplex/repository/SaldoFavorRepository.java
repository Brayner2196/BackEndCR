package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.SaldoFavor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SaldoFavorRepository extends JpaRepository<SaldoFavor, Long> {
    Optional<SaldoFavor> findByPropiedadId(Long propiedadId);
}
