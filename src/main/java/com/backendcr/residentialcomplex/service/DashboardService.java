package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.dashboard.*;
import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.EstadoReserva;
import com.backendcr.residentialcomplex.entity.PeriodoCobro;
import com.backendcr.residentialcomplex.repository.CobroRepository;
import com.backendcr.residentialcomplex.repository.PQRRepository;
import com.backendcr.residentialcomplex.repository.PagoRepository;
import com.backendcr.residentialcomplex.repository.PeriodoCobroRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final String[] MESES_CORTOS = {
            "ENE", "FEB", "MAR", "ABR", "MAY", "JUN",
            "JUL", "AGO", "SEP", "OCT", "NOV", "DIC"
    };

    private final CobroRepository cobroRepo;
    private final PagoRepository pagoRepo;
    private final PQRRepository pqrRepo;
    private final ReservaRepository reservaRepo;
    private final PropiedadRepository propiedadRepo;
    private final PeriodoCobroRepository periodoRepo;

    public DashboardResumenResponse resumen() {
        return new DashboardResumenResponse(
                pendientesHoy(),
                recaudoMes(),
                carteraVencida(),
                pagosPorVerificar(),
                tendencia(),
                estadoUnidades()
        );
    }

    public PendientesHoyResponse pendientesHoy() {
        long comprobantes = pagoRepo.findAllByEstado(EstadoPago.PENDIENTE_VERIFICACION).size();
        long pqrs = pqrRepo.countByEstado(EstadoPQR.PENDIENTE);
        long reservas = reservaRepo.countByEstado(EstadoReserva.PENDIENTE);
        return new PendientesHoyResponse(comprobantes, pqrs, reservas,
                comprobantes + pqrs + reservas);
    }

    public PagosPorVerificarResponse pagosPorVerificar() {
        return new PagosPorVerificarResponse(
                pagoRepo.findAllByEstado(EstadoPago.PENDIENTE_VERIFICACION).size());
    }

    public RecaudoMesResponse recaudoMes() {
        YearMonth ahora = YearMonth.now();
        YearMonth anterior = ahora.minusMonths(1);
        var cobrosPorMes = agruparCobrosPorMes();
        int actual = porcentajeRecaudo(cobrosPorMes.get(ahora));
        int previo = porcentajeRecaudo(cobrosPorMes.get(anterior));
        BigDecimal recaudado = sumaPagado(cobrosPorMes.get(ahora));
        BigDecimal esperado = sumaTotal(cobrosPorMes.get(ahora));
        return new RecaudoMesResponse(
                ahora.getYear(), ahora.getMonthValue(),
                actual, actual - previo, recaudado, esperado);
    }

    public CarteraVencidaResponse carteraVencida() {
        LocalDate hoy = LocalDate.now();
        BigDecimal montoActual = BigDecimal.ZERO;
        BigDecimal montoMesAnterior = BigDecimal.ZERO;
        long unidades = 0;
        java.util.Set<Long> propiedadesEnMora = new java.util.HashSet<>();
        LocalDate finMesAnterior = hoy.withDayOfMonth(1).minusDays(1);

        for (Cobro c : cobroRepo.findAll()) {
            if (estaVencido(c, hoy)) {
                montoActual = montoActual.add(montoSafe(c.getMontoTotal()));
                propiedadesEnMora.add(c.getPropiedadId());
            }
            if (estaVencido(c, finMesAnterior)) {
                montoMesAnterior = montoMesAnterior.add(montoSafe(c.getMontoTotal()));
            }
        }
        unidades = propiedadesEnMora.size();
        return new CarteraVencidaResponse(montoActual,
                montoActual.subtract(montoMesAnterior), unidades);
    }

    public TendenciaResponse tendencia() {
        var cobrosPorMes = agruparCobrosPorMes();
        List<TendenciaMesDto> meses = new ArrayList<>();
        YearMonth cursor = YearMonth.now().minusMonths(5);
        for (int i = 0; i < 6; i++) {
            int pct = porcentajeRecaudo(cobrosPorMes.get(cursor));
            meses.add(new TendenciaMesDto(
                    cursor.getYear(), cursor.getMonthValue(),
                    MESES_CORTOS[cursor.getMonthValue() - 1], pct));
            cursor = cursor.plusMonths(1);
        }
        String tendenciaTxt = clasificarTendencia(meses);
        return new TendenciaResponse(meses, tendenciaTxt);
    }

    public EstadoUnidadesResponse estadoUnidades() {
        long total = propiedadRepo.countPropiedadesIsFacturable();
        LocalDate hoy = LocalDate.now();
        LocalDate proximaSemana = hoy.plusDays(7);
        java.util.Set<Long> mora = new java.util.HashSet<>();
        java.util.Set<Long> porVencer = new java.util.HashSet<>();

        for (Cobro c : cobroRepo.findAll()) {
            if (c.getPropiedadId() == null) continue;
            if (estaVencido(c, hoy)) {
                mora.add(c.getPropiedadId());
            } else if (esPendienteCercano(c, hoy, proximaSemana)) {
                porVencer.add(c.getPropiedadId());
            }
        }
        porVencer.removeAll(mora);
        long enMora = mora.size();
        long pVencer = porVencer.size();
        long alDia = Math.max(0, total - enMora - pVencer);
        return new EstadoUnidadesResponse(total, alDia, pVencer, enMora);
    }

    // ─── Helpers ──────────────────────────────────

    /**
     * Agrupa cobros por el mes/año de su PERÍODO (no por fechaLimitePago).
     * Así la tendencia refleja en qué período de facturación pertenece cada cobro,
     * independientemente de cuándo vence o se paga.
     * Los cobros especiales sin período (multas, sanciones) se agrupan por fechaLimitePago
     * como fallback para no perder esa información en el gráfico.
     */
    private Map<YearMonth, List<Cobro>> agruparCobrosPorMes() {
        // Pre-cargar todos los períodos para evitar N+1 queries
        Map<Long, PeriodoCobro> periodosById = periodoRepo.findAll()
                .stream()
                .collect(Collectors.toMap(PeriodoCobro::getId, p -> p));

        Map<YearMonth, List<Cobro>> mapa = new java.util.HashMap<>();
        for (Cobro c : cobroRepo.findAll()) {
            YearMonth ym;
            if (c.getPeriodoId() != null && periodosById.containsKey(c.getPeriodoId())) {
                PeriodoCobro p = periodosById.get(c.getPeriodoId());
                ym = YearMonth.of(p.getAnio(), p.getMes());
            } else if (c.getFechaLimitePago() != null) {
                // Cobros especiales (multas/sanciones) sin período → fallback a fechaLimitePago
                ym = YearMonth.from(c.getFechaLimitePago());
            } else {
                continue;
            }
            mapa.computeIfAbsent(ym, k -> new ArrayList<>()).add(c);
        }
        return mapa;
    }

    private int porcentajeRecaudo(List<Cobro> cobros) {
        if (cobros == null || cobros.isEmpty()) return 0;
        BigDecimal total = sumaTotal(cobros);
        BigDecimal pagado = sumaPagado(cobros);
        if (total.signum() == 0) return 0;
        return pagado.multiply(BigDecimal.valueOf(100))
                .divide(total, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    private BigDecimal sumaTotal(List<Cobro> cobros) {
        if (cobros == null) return BigDecimal.ZERO;
        return cobros.stream()
                .map(c -> montoSafe(c.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumaPagado(List<Cobro> cobros) {
        if (cobros == null) return BigDecimal.ZERO;
        return cobros.stream()
                .filter(c -> c.getEstado() == EstadoCobro.PAGADO || c.getEstado() == EstadoCobro.EXONERADO)
                .map(c -> montoSafe(c.getMontoTotal()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal montoSafe(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private boolean estaVencido(Cobro c, LocalDate referencia) {
        return c.getFechaLimitePago() != null
                && c.getFechaLimitePago().isBefore(referencia)
                && c.getEstado() != EstadoCobro.PAGADO
                && c.getEstado() != EstadoCobro.EXONERADO;
    }

    private boolean esPendienteCercano(Cobro c, LocalDate hoy, LocalDate limite) {
        return c.getEstado() == EstadoCobro.PENDIENTE
                && c.getFechaLimitePago() != null
                && !c.getFechaLimitePago().isBefore(hoy)
                && !c.getFechaLimitePago().isAfter(limite);
    }

    private String clasificarTendencia(List<TendenciaMesDto> meses) {
        if (meses.size() < 2) return "ESTABLE";
        int primero = meses.get(0).porcentaje();
        int ultimo = meses.get(meses.size() - 1).porcentaje();
        int delta = ultimo - primero;
        if (delta >= 5) return "MEJORANDO";
        if (delta <= -5) return "EMPEORANDO";
        return "ESTABLE";
    }

    @SuppressWarnings("unused")
    private static String mesNombreLargo(int mes) {
    	return Month.of(mes)
                .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es"));
    }
}
