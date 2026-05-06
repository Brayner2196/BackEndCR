package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface PagoRepository extends JpaRepository<Pago, Long> {
    List<Pago> findAllByEstado(EstadoPago estado);
    List<Pago> findAllByUsuarioId(Long usuarioId);
    List<Pago> findAllByCobroId(Long cobroId);
    Optional<Pago> findByCobroIdAndEstado(Long cobroId, EstadoPago estado);
    boolean existsByCobroId(Long cobroId);

    /** Devuelve los cobroIds (del conjunto dado) que tienen al menos un Pago. */
    @Query("SELECT DISTINCT p.cobroId FROM Pago p WHERE p.cobroId IN :cobroIds")
    Set<Long> findCobroIdsWithPagos(@Param("cobroIds") Collection<Long> cobroIds);
}
