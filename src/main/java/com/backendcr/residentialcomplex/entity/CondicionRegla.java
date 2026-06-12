package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.entity.enums.CampoCartera;
import com.backendcr.residentialcomplex.entity.enums.OperadorComparacion;
import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * Condición atómica de una regla: compara un {@link CampoCartera} de las métricas
 * de la propiedad contra un {@link #valor} usando un {@link OperadorComparacion}.
 * Ej: DIAS_VENCIDO_MAX MAYOR_IGUAL 30.
 */
@Entity
@Table(name = "condiciones_regla")
public class CondicionRegla {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "regla_id", nullable = false)
    private Long reglaId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CampoCartera campo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private OperadorComparacion operador;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal valor;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getReglaId() { return reglaId; }
    public void setReglaId(Long reglaId) { this.reglaId = reglaId; }

    public CampoCartera getCampo() { return campo; }
    public void setCampo(CampoCartera campo) { this.campo = campo; }

    public OperadorComparacion getOperador() { return operador; }
    public void setOperador(OperadorComparacion operador) { this.operador = operador; }

    public BigDecimal getValor() { return valor; }
    public void setValor(BigDecimal valor) { this.valor = valor; }
}
