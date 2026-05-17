package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Respuesta de un residente a una votación.
 * - Para OPCION_UNICA:    opcionId con el id de la opción elegida
 * - Para OPCION_MULTIPLE: múltiples filas, una por opción seleccionada (mismo residenteId + votacionId)
 * - Para ESCALA_NUMERICA: valorNumerico con el número elegido
 * - Para TEXTO_LIBRE:     respuestaTexto con la respuesta
 */
@Entity
@Table(name = "votos_residentes")
public class VotoResidente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "votacion_id", nullable = false)
    private Long votacionId;

    @Column(name = "residente_id", nullable = false)
    private Long residenteId;

    @Column(name = "residente_nombre", length = 150)
    private String residenteNombre;

    /** Para OPCION_UNICA y OPCION_MULTIPLE */
    @Column(name = "opcion_id")
    private Long opcionId;

    /** Para ESCALA_NUMERICA */
    @Column(name = "valor_numerico")
    private Integer valorNumerico;

    /** Para TEXTO_LIBRE */
    @Column(name = "respuesta_texto", length = 1000)
    private String respuestaTexto;

    @CreationTimestamp
    @Column(name = "votado_en", nullable = false, updatable = false)
    private LocalDateTime votadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVotacionId() { return votacionId; }
    public void setVotacionId(Long votacionId) { this.votacionId = votacionId; }

    public Long getResidenteId() { return residenteId; }
    public void setResidenteId(Long residenteId) { this.residenteId = residenteId; }

    public String getResidenteNombre() { return residenteNombre; }
    public void setResidenteNombre(String residenteNombre) { this.residenteNombre = residenteNombre; }

    public Long getOpcionId() { return opcionId; }
    public void setOpcionId(Long opcionId) { this.opcionId = opcionId; }

    public Integer getValorNumerico() { return valorNumerico; }
    public void setValorNumerico(Integer valorNumerico) { this.valorNumerico = valorNumerico; }

    public String getRespuestaTexto() { return respuestaTexto; }
    public void setRespuestaTexto(String respuestaTexto) { this.respuestaTexto = respuestaTexto; }

    public LocalDateTime getVotadoEn() { return votadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
