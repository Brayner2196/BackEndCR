package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.EstadoPQR;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pqr_historial")
public class PQRHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pqr_id", nullable = false)
    private Long pqrId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoPQR estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoPQR estadoNuevo;

    @Column(name = "cambiado_por")
    private Long cambiadoPor;

    @Column(length = 500)
    private String comentario;

    @CreationTimestamp
    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    public PQRHistorial() {}

    public PQRHistorial(Long pqrId, EstadoPQR estadoAnterior, EstadoPQR estadoNuevo,
                        Long cambiadoPor, String comentario) {
        this.pqrId = pqrId;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.cambiadoPor = cambiadoPor;
        this.comentario = comentario;
    }

    public Long getId() { return id; }
    public Long getPqrId() { return pqrId; }
    public void setPqrId(Long pqrId) { this.pqrId = pqrId; }
    public EstadoPQR getEstadoAnterior() { return estadoAnterior; }
    public void setEstadoAnterior(EstadoPQR estadoAnterior) { this.estadoAnterior = estadoAnterior; }
    public EstadoPQR getEstadoNuevo() { return estadoNuevo; }
    public void setEstadoNuevo(EstadoPQR estadoNuevo) { this.estadoNuevo = estadoNuevo; }
    public Long getCambiadoPor() { return cambiadoPor; }
    public void setCambiadoPor(Long cambiadoPor) { this.cambiadoPor = cambiadoPor; }
    public String getComentario() { return comentario; }
    public void setComentario(String comentario) { this.comentario = comentario; }
    public LocalDateTime getFechaCambio() { return fechaCambio; }
}
