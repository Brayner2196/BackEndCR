package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.MovimientoAbono;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovimientoAbonoRepository extends JpaRepository<MovimientoAbono, Long> {
    List<MovimientoAbono> findAllByAbonoId(Long abonoId);
    List<MovimientoAbono> findAllByCobroId(Long cobroId);
}
