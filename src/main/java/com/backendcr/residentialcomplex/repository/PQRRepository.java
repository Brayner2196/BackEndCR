package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PQRRepository extends JpaRepository<PQR, Long> {
    List<PQR> findAllByEstado(EstadoPQR estado);
    List<PQR> findAllByResidenteId(Long residenteId);
    List<PQR> findAllByResidenteIdAndEstado(Long residenteId, EstadoPQR estado);
    long countByEstado(EstadoPQR estado);
}
