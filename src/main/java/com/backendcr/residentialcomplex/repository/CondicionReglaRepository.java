package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.CondicionRegla;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CondicionReglaRepository extends JpaRepository<CondicionRegla, Long> {

    List<CondicionRegla> findByReglaId(Long reglaId);
}
