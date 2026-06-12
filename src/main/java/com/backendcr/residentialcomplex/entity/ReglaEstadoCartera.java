package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.OperadorLogico;
import jakarta.persistence.*;

/**
 * Regla de entrada a un estado de cartera. Un estado puede tener varias reglas;
 * basta que UNA se cumpla para entrar al estado (OR entre reglas). Dentro de la
 * regla, sus condiciones (cargadas por repositorio) se combinan según
 * {@link #operadorLogico}.
 */
@Entity
@Table(name = "reglas_estado_cartera")
public class ReglaEstadoCartera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "estado_cartera_id", nullable = false)
    private Long estadoCarteraId;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "operador_logico", nullable = false, length = 5)
    private OperadorLogico operadorLogico = OperadorLogico.AND;

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean activa = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEstadoCarteraId() { return estadoCarteraId; }
    public void setEstadoCarteraId(Long estadoCarteraId) { this.estadoCarteraId = estadoCarteraId; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public OperadorLogico getOperadorLogico() { return operadorLogico; }
    public void setOperadorLogico(OperadorLogico operadorLogico) { this.operadorLogico = operadorLogico; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }
}
