package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Parqueadero;
import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParqueaderoRepository extends JpaRepository<Parqueadero, Long> {

    boolean existsByIdentificador(String identificador);

    List<Parqueadero> findAllByTipo(TipoParqueadero tipo);

    List<Parqueadero> findAllByPropiedadId(Long propiedadId);

    Optional<Parqueadero> findByVehiculoId(Long vehiculoId);

    boolean existsByPropiedadParqueaderoId(Long propiedadParqueaderoId);
}
