package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Parametrización del módulo de vigilancia por conjunto (una sola fila por tenant).
 */
@Entity
@Table(name = "config_vigilancia")
public class ConfigVigilancia {

    @Id
    private Long id = 1L; // singleton por schema

    @Column(name = "expiracion_visita_horas", nullable = false)
    private int expiracionVisitaHoras = 24;

    @Column(name = "exige_documento_peatonal", nullable = false)
    private boolean exigeDocumentoPeatonal = true;

    @Column(name = "exige_foto_paquete", nullable = false)
    private boolean exigeFotoPaquete = false;

    @Column(name = "notificar_llegada_paquete", nullable = false)
    private boolean notificarLlegadaPaquete = true;

    /** Si una unidad está restringida por cartera, ¿puede el vigilante aprobar igual? */
    @Column(name = "permitir_aprobar_cartera_restringida", nullable = false)
    private boolean permitirAprobarConCarteraRestringida = false;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getExpiracionVisitaHoras() { return expiracionVisitaHoras; }
    public void setExpiracionVisitaHoras(int expiracionVisitaHoras) { this.expiracionVisitaHoras = expiracionVisitaHoras; }

    public boolean isExigeDocumentoPeatonal() { return exigeDocumentoPeatonal; }
    public void setExigeDocumentoPeatonal(boolean exigeDocumentoPeatonal) { this.exigeDocumentoPeatonal = exigeDocumentoPeatonal; }

    public boolean isExigeFotoPaquete() { return exigeFotoPaquete; }
    public void setExigeFotoPaquete(boolean exigeFotoPaquete) { this.exigeFotoPaquete = exigeFotoPaquete; }

    public boolean isNotificarLlegadaPaquete() { return notificarLlegadaPaquete; }
    public void setNotificarLlegadaPaquete(boolean notificarLlegadaPaquete) { this.notificarLlegadaPaquete = notificarLlegadaPaquete; }

    public boolean isPermitirAprobarConCarteraRestringida() { return permitirAprobarConCarteraRestringida; }
    public void setPermitirAprobarConCarteraRestringida(boolean v) { this.permitirAprobarConCarteraRestringida = v; }

    public Instant getActualizadoEn() { return actualizadoEn; }
}
