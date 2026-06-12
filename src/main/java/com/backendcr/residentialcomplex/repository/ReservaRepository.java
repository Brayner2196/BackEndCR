package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Reserva;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findAllByEstado(EstadoReserva estado);
    List<Reserva> findAllByResidenteId(Long residenteId);
    List<Reserva> findAllByZonaComunIdAndFecha(Long zonaComunId, LocalDate fecha);
    long countByEstado(EstadoReserva estado);

    /**
     * Cuenta las reservas activas (PENDIENTE o APROBADA) de una zona en una fecha
     * que se solapan con el rango [horaInicio, horaFin).
     * Solapamiento: inicio < horaFin AND fin > horaInicio
     */
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.zonaComunId = :zonaId
              AND r.fecha = :fecha
              AND r.estado IN ('PENDIENTE', 'APROBADA')
              AND r.horaInicio < :horaFin
              AND r.horaFin   > :horaInicio
            """)
    long countSolapamientos(
            @Param("zonaId") Long zonaId,
            @Param("fecha") LocalDate fecha,
            @Param("horaInicio") LocalTime horaInicio,
            @Param("horaFin") LocalTime horaFin
    );

    /**
     * Cuenta las reservas activas (PENDIENTE o APROBADA) de un residente en una
     * zona dentro de un rango de fechas [desde, hasta]. Usado para validar las
     * cuotas por residente (máximo por semana / por mes).
     */
    @Query("""
            SELECT COUNT(r) FROM Reserva r
            WHERE r.residenteId = :residenteId
              AND r.zonaComunId = :zonaId
              AND r.estado IN ('PENDIENTE', 'APROBADA')
              AND r.fecha BETWEEN :desde AND :hasta
            """)
    long countActivasResidenteZonaEnRango(
            @Param("residenteId") Long residenteId,
            @Param("zonaId") Long zonaId,
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta
    );
}
