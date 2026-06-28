package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.ResultadoAcceso;
import com.backendcr.residentialcomplex.entity.enums.TipoEventoAcceso;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Minuta inmutable de eventos de portería: accesos vehiculares/peatonales,
 * validación de visitas y movimientos de paquetería. Solo se inserta, no se edita.
 */
@Entity
@Table(name = "bitacora_acceso")
public class BitacoraAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 25)
    private TipoEventoAcceso tipoEvento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private ResultadoAcceso resultado;

    @Column(length = 300)
    private String descripcion;

    @Column(name = "propiedad_id")
    private Long propiedadId;

    @Column(length = 15)
    private String placa;

    @Column(length = 30)
    private String documento;

    @Column(name = "nombre_visitante", length = 120)
    private String nombreVisitante;

    @Column(name = "vigilante_id")
    private Long vigilanteId;

    @Column(name = "visita_id")
    private Long visitaId;

    @Column(name = "paquete_id")
    private Long paqueteId;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public TipoEventoAcceso getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(TipoEventoAcceso tipoEvento) { this.tipoEvento = tipoEvento; }

    public ResultadoAcceso getResultado() { return resultado; }
    public void setResultado(ResultadoAcceso resultado) { this.resultado = resultado; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Long getPropiedadId() { return propiedadId; }
    public void setPropiedadId(Long propiedadId) { this.propiedadId = propiedadId; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }

    public String getNombreVisitante() { return nombreVisitante; }
    public void setNombreVisitante(String nombreVisitante) { this.nombreVisitante = nombreVisitante; }

    public Long getVigilanteId() { return vigilanteId; }
    public void setVigilanteId(Long vigilanteId) { this.vigilanteId = vigilanteId; }

    public Long getVisitaId() { return visitaId; }
    public void setVisitaId(Long visitaId) { this.visitaId = visitaId; }

    public Long getPaqueteId() { return paqueteId; }
    public void setPaqueteId(Long paqueteId) { this.paqueteId = paqueteId; }

    public Instant getCreadoEn() { return creadoEn; }
}
