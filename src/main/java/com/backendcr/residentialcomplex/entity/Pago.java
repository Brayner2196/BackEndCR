package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoPago;
import com.backendcr.residentialcomplex.entity.enums.MetodoPago;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Entity
@Table(
    name = "pagos",
    uniqueConstraints = {
        // Evita duplicados cuando el webhook y el WebView callback llegan simultáneamente.
        // Si dos threads intentan guardar el mismo paymentId/transactionId, el segundo
        // recibe DataIntegrityViolationException que PagoService captura y descarta.
        @UniqueConstraint(name = "uk_pagos_referencia", columnNames = {"referencia"})
    }
)
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cobro_id", nullable = false)
    private Long cobroId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "monto_pagado", nullable = false, precision = 12, scale = 0)
    private BigDecimal montoPagado;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, columnDefinition = "varchar(20)")
    private MetodoPago metodoPago;

    // Longitud 150 para cubrir prefijos como "MP-", "WOMPI-", "BOLD-" + IDs largos
    @Column(length = 150, unique = true)
    private String referencia;

    @Column(name = "url_comprobante", length = 500)
    private String urlComprobante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EstadoPago estado = EstadoPago.PENDIENTE_VERIFICACION;

    @Column(name = "verificado_por")
    private Long verificadoPor;

    @Column(name = "fecha_verificacion")
    private Instant fechaVerificacion;

    @Column(name = "motivo_rechazo", length = 300)
    private String motivoRechazo;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCobroId() { return cobroId; }
    public void setCobroId(Long cobroId) { this.cobroId = cobroId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public BigDecimal getMontoPagado() { return montoPagado; }
    public void setMontoPagado(BigDecimal monto) { this.montoPagado = monto; }
    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate d) { this.fechaPago = d; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String referencia) { this.referencia = referencia; }
    public String getUrlComprobante() { return urlComprobante; }
    public void setUrlComprobante(String url) { this.urlComprobante = url; }
    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }
    public Long getVerificadoPor() { return verificadoPor; }
    public void setVerificadoPor(Long id) { this.verificadoPor = id; }
    public Instant getFechaVerificacion() { return fechaVerificacion; }
    public void setFechaVerificacion(Instant dt) { this.fechaVerificacion = dt; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String motivo) { this.motivoRechazo = motivo; }
    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
