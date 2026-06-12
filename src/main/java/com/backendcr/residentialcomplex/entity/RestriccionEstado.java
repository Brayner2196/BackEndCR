package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import jakarta.persistence.*;

/**
 * Restricción que un estado de cartera impone: la sola existencia de esta fila
 * significa que la {@link #accion} está bloqueada cuando la propiedad está en el
 * estado {@link #estadoCarteraId}.
 */
@Entity
@Table(name = "restricciones_estado",
       uniqueConstraints = @UniqueConstraint(columnNames = {"estado_cartera_id", "accion"}))
public class RestriccionEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estado_cartera_id", nullable = false)
    private Long estadoCarteraId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AccionRestringible accion;

    /** Mensaje mostrado al usuario o al vigilante cuando se bloquea. */
    @Column(length = 200)
    private String mensaje;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEstadoCarteraId() { return estadoCarteraId; }
    public void setEstadoCarteraId(Long estadoCarteraId) { this.estadoCarteraId = estadoCarteraId; }

    public AccionRestringible getAccion() { return accion; }
    public void setAccion(AccionRestringible accion) { this.accion = accion; }

    public String getMensaje() { return mensaje; }
    public void setMensaje(String mensaje) { this.mensaje = mensaje; }
}
