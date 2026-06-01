package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ConfiguracionParqueadero;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConfiguracionParqueaderoRepository extends JpaRepository<ConfiguracionParqueadero, Long> {
    Optional<ConfiguracionParqueadero> findFirstBy();
}
