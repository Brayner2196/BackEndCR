package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Vehiculo;
import com.backendcr.residentialcomplex.entity.enums.EstadoVehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    List<Vehiculo> findAllByPropiedadId(Long propiedadId);

    List<Vehiculo> findAllByEstado(EstadoVehiculo estado);

    long countByPropiedadIdAndEstadoNot(Long propiedadId, EstadoVehiculo estado);

    boolean existsByPlacaAndPropiedadId(String placa, Long propiedadId);
}
