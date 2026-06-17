package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Auditoría de avisos de cobranza enviados a los residentes de una propiedad
 * (recordatorio de fase, paso a cartera, pre-jurídico, etc.).
 *
 * Una fila por cada envío, para trazabilidad de la gestión de morosos.
 */
@Entity
@Table(name = "aviso_cobranza")
public class AvisoCobranza {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "propiedad_id", nullable = false)
    private Long propiedadId;

    /** Fase de cartera referida en el aviso. Null si fue un aviso genérico. */
    @Column(name = "estado_cartera_id")
    private Long estadoCarteraId;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 500)
    private String mensaje;

    @Column(name = "usuarios_notificados", nullable = false)
    private int usuariosNotificados;

    /** Email del admin que envió el aviso. */
    @Column(name = "enviado_por", length = 150)
    private String enviadoPor;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public Long getEstadoCarteraId() { return estadoCarteraId; }
    public void setEstadoCarteraId(Long estadoCarteraId) { this.estadoCarteraId = estadoCarteraId; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }

    public int getUsuariosNotificados() { return usuariosNotificados; }
    public void setUsuariosNotificados(int usuariosNotificados) { this.usuariosNotificados = usuariosNotificados; }

    public String getEnviadoPor() { return enviadoPor; }
    public void setEnviadoPor(String enviadoPor) { this.enviadoPor = enviadoPor; }

    public Instant getCreadoEn() { return creadoEn; }
}
