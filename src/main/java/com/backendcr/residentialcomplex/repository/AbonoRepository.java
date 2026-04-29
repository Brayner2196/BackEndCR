package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Abono;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AbonoRepository extends JpaRepository<Abono, Long> {
    List<Abono> findAllByEstado(EstadoPago estado);
    List<Abono> findAllByUsuarioId(Long usuarioId);
    List<Abono> findAllByPropiedadId(Long propiedadId);
}
