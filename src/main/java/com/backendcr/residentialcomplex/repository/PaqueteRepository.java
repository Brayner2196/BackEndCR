package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Paquete;
import com.backendcr.residentialcomplex.entity.enums.EstadoPaquete;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaqueteRepository extends JpaRepository<Paquete, Long> {

    List<Paquete> findAllByEstadoOrderByRecibidoEnDesc(EstadoPaquete estado);

    List<Paquete> findAllByPropiedadIdOrderByRecibidoEnDesc(Long propiedadId);

    List<Paquete> findAllByPropiedadIdInOrderByRecibidoEnDesc(List<Long> propiedadIds);

    long countByEstado(EstadoPaquete estado);
}
