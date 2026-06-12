package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.ReglaEstadoCartera;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaEstadoCarteraRepository extends JpaRepository<ReglaEstadoCartera, Long> {

    List<ReglaEstadoCartera> findByEstadoCarteraIdAndActivaTrueOrderByOrdenAsc(Long estadoCarteraId);

    List<ReglaEstadoCartera> findByEstadoCarteraIdOrderByOrdenAsc(Long estadoCarteraId);

    void deleteByEstadoCarteraId(Long estadoCarteraId);
}
