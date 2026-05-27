package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Pago;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
    boolean existsByReferencia(String referencia);

    /**
     * Igual que findAllByCobroId pero adquiere un lock pesimista de escritura.
     * Usado en PagoService.registrarYVerificarPago* para garantizar idempotencia
     * cuando el webhook y el WebView callback llegan al mismo tiempo.
     * Solo debe llamarse dentro de una transacción activa (@Transactional).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Pago p WHERE p.cobroId = :cobroId")
    List<Pago> findAllByCobroIdForUpdate(@Param("cobroId") Long cobroId);

    /** Devuelve los cobroIds (del conjunto dado) que tienen al menos un Pago. */
    @Query("SELECT DISTINCT p.cobroId FROM Pago p WHERE p.cobroId IN :cobroIds")
    Set<Long> findCobroIdsWithPagos(@Param("cobroIds") Collection<Long> cobroIds);
}
