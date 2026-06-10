package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PQRRepository extends JpaRepository<PQR, Long> {
    List<PQR> findAllByEstado(EstadoPQR estado);
    List<PQR> findAllByResidenteId(Long residenteId);
    List<PQR> findAllByResidenteIdAndEstado(Long residenteId, EstadoPQR estado);
    long countByEstado(EstadoPQR estado);

    @Query("SELECT p FROM PQR p WHERE p.creadoEn >= :desde AND p.creadoEn <= :hasta")
    List<PQR> findByCreadoEnBetween(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
