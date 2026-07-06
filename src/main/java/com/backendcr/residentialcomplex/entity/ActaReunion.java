package com.backendcr.residentialcomplex.entity;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.backendcr.residentialcomplex.entity.enums.EstadoActa;

import jakarta.persistence.*;

/**
 * Acta de reunión del consejo comunal generada a partir de una grabación
 * de voz transcrita con Whisper (local, en el backend).
 *
 * - `transcripcion` guarda el texto crudo que produjo Whisper (auditoría).
 * - `contenido` es la versión editable que el presidente ajusta antes de finalizar.
 *
 * Vive dentro del schema tenant (no en public).
 * Instantes en UTC (Instant/timestamptz) según el lineamiento del proyecto.
 */
@Entity
@Table(name = "acta_reunion")
public class ActaReunion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "titulo", nullable = false, length = 200)
    private String titulo;

    /** Instante de la reunión (UTC). */
    @Column(name = "fecha_reunion", nullable = false)
    private Instant fechaReunion;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoActa estado = EstadoActa.PROCESANDO;

    /** Texto crudo producido por Whisper. Inmutable tras la transcripción. */
    @Column(name = "transcripcion", columnDefinition = "TEXT")
    private String transcripcion;

    /** Contenido editable del acta (parte de la transcripción y el presidente lo ajusta). */
    @Column(name = "contenido", columnDefinition = "TEXT")
    private String contenido;

    /** Ruta del archivo de audio original en el servidor. */
    @Column(name = "audio_path", length = 500)
    private String audioPath;

    /** Duración de la grabación reportada por la app (segundos). */
    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    /** Usuario (presidente) que creó el acta. */
    @Column(name = "creado_por_usuario_id", nullable = false)
    private Long creadoPorUsuarioId;

    /** Mensaje de error si la transcripción falló. */
    @Column(name = "error_mensaje", length = 500)
    private String errorMensaje;

    @Column(name = "finalizada_en")
    private Instant finalizadaEn;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    // ─── Getters / Setters ────────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }

    public Instant getFechaReunion() { return fechaReunion; }
    public void setFechaReunion(Instant fechaReunion) { this.fechaReunion = fechaReunion; }

    public EstadoActa getEstado() { return estado; }
    public void setEstado(EstadoActa estado) { this.estado = estado; }

    public String getTranscripcion() { return transcripcion; }
    public void setTranscripcion(String transcripcion) { this.transcripcion = transcripcion; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getAudioPath() { return audioPath; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }

    public Integer getDuracionSegundos() { return duracionSegundos; }
    public void setDuracionSegundos(Integer duracionSegundos) { this.duracionSegundos = duracionSegundos; }

    public Long getCreadoPorUsuarioId() { return creadoPorUsuarioId; }
    public void setCreadoPorUsuarioId(Long creadoPorUsuarioId) { this.creadoPorUsuarioId = creadoPorUsuarioId; }

    public String getErrorMensaje() { return errorMensaje; }
    public void setErrorMensaje(String errorMensaje) { this.errorMensaje = errorMensaje; }

    public Instant getFinalizadaEn() { return finalizadaEn; }
    public void setFinalizadaEn(Instant finalizadaEn) { this.finalizadaEn = finalizadaEn; }

    public Instant getCreadoEn() { return creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
}
