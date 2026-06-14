package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.consejo.ConsejoEstadisticasResponse;
import com.backendcr.residentialcomplex.entity.Anuncio;
import com.backendcr.residentialcomplex.entity.PQR;
import com.backendcr.residentialcomplex.entity.Votacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoAnuncio;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.entity.enums.EstadoVotacion;
import com.backendcr.residentialcomplex.repository.AnuncioRepository;
import com.backendcr.residentialcomplex.repository.AnuncioVistaRepository;
import com.backendcr.residentialcomplex.repository.PQRRepository;
import com.backendcr.residentialcomplex.repository.VotacionRepository;
import com.backendcr.residentialcomplex.repository.VotoResidenteRepository;
import com.backendcr.residentialcomplex.config.TenantClock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConsejoEstadisticasService {

    private final PQRRepository pqrRepository;
    private final AnuncioRepository anuncioRepository;
    private final AnuncioVistaRepository anuncioVistaRepository;
    private final VotacionRepository votacionRepository;
    private final VotoResidenteRepository votoResidenteRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ConsejoEstadisticasResponse calcular(Instant desde, Instant hasta) {

        // ── PQRs ─────────────────────────────────────────────────────────────
        List<PQR> pqrs = pqrRepository.findByCreadoEnBetween(desde, hasta);

        Map<String, Long> pqrPorEstado = pqrs.stream()
                .collect(Collectors.groupingBy(p -> p.getEstado().name(), Collectors.counting()));

        Map<String, Long> pqrPorTipo = pqrs.stream()
                .collect(Collectors.groupingBy(p -> p.getTipo().name(), Collectors.counting()));

        long pqrResueltas = pqrs.stream()
                .filter(p -> p.getEstado() == EstadoPQR.RESUELTO || p.getEstado() == EstadoPQR.CERRADO)
                .count();

        // Tiempo promedio de respuesta en horas (solo pqrs con fecha de respuesta)
        Double tiempoPromHoras = pqrs.stream()
                .filter(p -> p.getFechaRespuesta() != null)
                .mapToLong(p -> Duration.between(p.getCreadoEn(), p.getFechaRespuesta()).toHours())
                .average()
                .isPresent()
                ? pqrs.stream()
                        .filter(p -> p.getFechaRespuesta() != null)
                        .mapToLong(p -> Duration.between(p.getCreadoEn(), p.getFechaRespuesta()).toHours())
                        .average()
                        .getAsDouble()
                : null;

        // ── Anuncios ─────────────────────────────────────────────────────────
        List<Anuncio> anuncios = anuncioRepository.findByCreadoEnBetween(desde, hasta);

        long anuncioActivos = anuncios.stream()
                .filter(a -> a.getEstado() == EstadoAnuncio.ACTIVO)
                .count();

        List<Long> anuncioIds = anuncios.stream().map(Anuncio::getId).toList();
        long totalVistas = anuncioIds.isEmpty() ? 0L
                : anuncioVistaRepository.countByAnuncioIdIn(anuncioIds);

        // ── Votaciones ───────────────────────────────────────────────────────
        List<Votacion> votaciones = votacionRepository.findByCreadoEnBetween(desde, hasta);

        Map<String, Long> votacionPorEstado = votaciones.stream()
                .collect(Collectors.groupingBy(v -> v.getEstado().name(), Collectors.counting()));

        List<Long> votacionIds = votaciones.stream().map(Votacion::getId).toList();
        long participantes = votacionIds.isEmpty() ? 0L
                : votoResidenteRepository.countDistinctResidentesByVotacionIds(votacionIds);

        // ── Respuesta ────────────────────────────────────────────────────────
        return new ConsejoEstadisticasResponse(
                LocalDate.ofInstant(desde, TenantClock.zona()).format(FMT),
                LocalDate.ofInstant(hasta, TenantClock.zona()).format(FMT),
                pqrs.size(),
                pqrPorEstado,
                pqrPorTipo,
                pqrResueltas,
                tiempoPromHoras,
                anuncios.size(),
                anuncioActivos,
                totalVistas,
                votaciones.size(),
                votacionPorEstado,
                participantes
        );
    }
}
