package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.PQRHistorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PQRHistorialRepository extends JpaRepository<PQRHistorial, Long> {
    List<PQRHistorial> findAllByPqrIdOrderByFechaCambioAsc(Long pqrId);
}
