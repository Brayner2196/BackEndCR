package com.backendcr.residentialcomplex.repository;

import com.backendcr.residentialcomplex.entity.Reserva;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {
    List<Reserva> findAllByEstado(EstadoReserva estado);
    List<Reserva> findAllByResidenteId(Long residenteId);
    List<Reserva> findAllByZonaComunIdAndFecha(Long zonaComunId, LocalDate fecha);
    long countByEstado(EstadoReserva estado);
}
