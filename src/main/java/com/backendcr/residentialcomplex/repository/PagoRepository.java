package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findAllByEstado(EstadoPago estado);
    List<Pago> findAllByUsuarioId(Long usuarioId);
    List<Pago> findAllByCobroId(Long cobroId);
    Optional<Pago> findByCobroIdAndEstado(Long cobroId, EstadoPago estado);
    boolean existsByCobroId(Long cobroId);
}
