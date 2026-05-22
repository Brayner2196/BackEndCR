package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.CategoriaZona;
import com.backendcr.residentialcomplex.entity.enums.ModoAprobacion;
import com.backendcr.residentialcomplex.entity.enums.ModoTarifa;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "zonas_comunes")
public class ZonaComun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 500)
    private String descripcion;

    @Column(nullable = false)
    private Integer capacidad = 0;

    @Column(nullable = false)
    private boolean activa = true;

    // ── Categoría e identidad ───────────────────────────────────
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CategoriaZona categoria = CategoriaZona.OTRO;

    // ── Horario legacy (se mantiene por compatibilidad) ─────────
    @Column(name = "hora_apertura")
    private LocalTime horaApertura;

    @Column(name = "hora_cierre")
    private LocalTime horaCierre;

    /** CSV de días: LUNES,MARTES,... Null = todos los días. */
    @Column(name = "dias_disponibles", length = 100)
    private String diasDisponibles;

    // ── Modo de uso ─────────────────────────────────────────────
    /** true = exclusivo (1 reserva bloquea toda la zona), false = compartido */
    @Column(name = "uso_exclusivo", nullable = false)
    private boolean usoExclusivo = true;

    /** Tiempo de limpieza entre reservas en minutos (0, 15, 30, 60) */
    @Column(name = "buffer_limpieza_minutos", nullable = false)
    private int bufferLimpiezaMinutos = 0;

    // ── Reglas de duración ───────────────────────────────────────
    @Column(name = "duracion_min_minutos")
    private Integer duracionMinMinutos;

    @Column(name = "duracion_max_minutos")
    private Integer duracionMaxMinutos;

    // ── Reglas de anticipación ───────────────────────────────────
    @Column(name = "anticipacion_min_dias")
    private Integer anticipacionMinDias;

    @Column(name = "anticipacion_max_dias")
    private Integer anticipacionMaxDias;

    // ── Cuota por residente ─────────────────────────────────────
    @Column(name = "max_reservas_semana")
    private Integer maxReservasSemana;

    @Column(name = "max_reservas_mes")
    private Integer maxReservasMes;

    /** Horas antes del inicio dentro de las que se permite cancelar */
    @Column(name = "cancelacion_horas_antes")
    private Integer cancelacionHorasAntes;

    // ── Aprobación ───────────────────────────────────────────────
    /** Campo legacy — se mantiene para compatibilidad (AUTOMATICA = false, MANUAL = true) */
    @Column(name = "requiere_aprobacion", nullable = false)
    private boolean requiereAprobacion = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_aprobacion", length = 15, nullable = false)
    private ModoAprobacion modoAprobacion = ModoAprobacion.MANUAL;

    // ── Costo ────────────────────────────────────────────────────
    @Column(name = "tiene_costo", nullable = false)
    private boolean tieneCosto = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_tarifa", length = 15)
    private ModoTarifa modoTarifa = ModoTarifa.FIJA;

    @Column(name = "tarifa_monto", precision = 14, scale = 2)
    private BigDecimal tarifaMonto;

    @Column(name = "deposito_monto", precision = 14, scale = 2)
    private BigDecimal depositoMonto;

    // ── Restricciones de acceso ──────────────────────────────────
    @Column(name = "solo_propietarios", nullable = false)
    private boolean soloPropietarios = false;

    @Column(name = "sin_deuda_pendiente", nullable = false)
    private boolean sinDeudaPendiente = false;

    @Column(name = "edad_minima")
    private Integer edadMinima;

    /** Restricción por torre/bloque. Null = sin restricción */
    @Column(name = "solo_torre", length = 50)
    private String soloTorre;

    // ── Suspensión ───────────────────────────────────────────────
    @Column(nullable = false)
    private boolean suspendida = false;

    @Column(name = "motivo_suspension", length = 300)
    private String motivoSuspension;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    // ── Getters / Setters ─────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Integer getCapacidad() { return capacidad; }
    public void setCapacidad(Integer capacidad) { this.capacidad = capacidad; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public CategoriaZona getCategoria() { return categoria; }
    public void setCategoria(CategoriaZona categoria) { this.categoria = categoria; }

    public LocalTime getHoraApertura() { return horaApertura; }
    public void setHoraApertura(LocalTime horaApertura) { this.horaApertura = horaApertura; }

    public LocalTime getHoraCierre() { return horaCierre; }
    public void setHoraCierre(LocalTime horaCierre) { this.horaCierre = horaCierre; }

    public String getDiasDisponibles() { return diasDisponibles; }
    public void setDiasDisponibles(String diasDisponibles) { this.diasDisponibles = diasDisponibles; }

    public boolean isUsoExclusivo() { return usoExclusivo; }
    public void setUsoExclusivo(boolean usoExclusivo) { this.usoExclusivo = usoExclusivo; }

    public int getBufferLimpiezaMinutos() { return bufferLimpiezaMinutos; }
    public void setBufferLimpiezaMinutos(int bufferLimpiezaMinutos) { this.bufferLimpiezaMinutos = bufferLimpiezaMinutos; }

    public Integer getDuracionMinMinutos() { return duracionMinMinutos; }
    public void setDuracionMinMinutos(Integer duracionMinMinutos) { this.duracionMinMinutos = duracionMinMinutos; }

    public Integer getDuracionMaxMinutos() { return duracionMaxMinutos; }
    public void setDuracionMaxMinutos(Integer duracionMaxMinutos) { this.duracionMaxMinutos = duracionMaxMinutos; }

    public Integer getAnticipacionMinDias() { return anticipacionMinDias; }
    public void setAnticipacionMinDias(Integer anticipacionMinDias) { this.anticipacionMinDias = anticipacionMinDias; }

    public Integer getAnticipacionMaxDias() { return anticipacionMaxDias; }
    public void setAnticipacionMaxDias(Integer anticipacionMaxDias) { this.anticipacionMaxDias = anticipacionMaxDias; }

    public Integer getMaxReservasSemana() { return maxReservasSemana; }
    public void setMaxReservasSemana(Integer maxReservasSemana) { this.maxReservasSemana = maxReservasSemana; }

    public Integer getMaxReservasMes() { return maxReservasMes; }
    public void setMaxReservasMes(Integer maxReservasMes) { this.maxReservasMes = maxReservasMes; }

    public Integer getCancelacionHorasAntes() { return cancelacionHorasAntes; }
    public void setCancelacionHorasAntes(Integer cancelacionHorasAntes) { this.cancelacionHorasAntes = cancelacionHorasAntes; }

    public boolean isRequiereAprobacion() { return requiereAprobacion; }
    public void setRequiereAprobacion(boolean requiereAprobacion) { this.requiereAprobacion = requiereAprobacion; }

    public ModoAprobacion getModoAprobacion() { return modoAprobacion; }
    public void setModoAprobacion(ModoAprobacion modoAprobacion) {
        this.modoAprobacion = modoAprobacion;
        this.requiereAprobacion = (modoAprobacion != ModoAprobacion.AUTOMATICA);
    }

    public boolean isTieneCosto() { return tieneCosto; }
    public void setTieneCosto(boolean tieneCosto) { this.tieneCosto = tieneCosto; }

    public ModoTarifa getModoTarifa() { return modoTarifa; }
    public void setModoTarifa(ModoTarifa modoTarifa) { this.modoTarifa = modoTarifa; }

    public BigDecimal getTarifaMonto() { return tarifaMonto; }
    public void setTarifaMonto(BigDecimal tarifaMonto) { this.tarifaMonto = tarifaMonto; }

    public BigDecimal getDepositoMonto() { return depositoMonto; }
    public void setDepositoMonto(BigDecimal depositoMonto) { this.depositoMonto = depositoMonto; }

    public boolean isSoloPropietarios() { return soloPropietarios; }
    public void setSoloPropietarios(boolean soloPropietarios) { this.soloPropietarios = soloPropietarios; }

    public boolean isSinDeudaPendiente() { return sinDeudaPendiente; }
    public void setSinDeudaPendiente(boolean sinDeudaPendiente) { this.sinDeudaPendiente = sinDeudaPendiente; }

    public Integer getEdadMinima() { return edadMinima; }
    public void setEdadMinima(Integer edadMinima) { this.edadMinima = edadMinima; }

    public String getSoloTorre() { return soloTorre; }
    public void setSoloTorre(String soloTorre) { this.soloTorre = soloTorre; }

    public boolean isSuspendida() { return suspendida; }
    public void setSuspendida(boolean suspendida) { this.suspendida = suspendida; }

    public String getMotivoSuspension() { return motivoSuspension; }
    public void setMotivoSuspension(String motivoSuspension) { this.motivoSuspension = motivoSuspension; }

    public LocalDateTime getCreadoEn() { return creadoEn; }
}
