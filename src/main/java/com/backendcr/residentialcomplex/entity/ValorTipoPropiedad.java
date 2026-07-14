package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;

/**
 * Catálogo de valores permitidos para un {@link TipoPropiedad}.
 *
 * <p>Modelo híbrido:</p>
 * <ul>
 *   <li><b>Plantilla global</b> ({@code parentValorId = null}): valores por
 *       defecto del tipo. Ej: tipo "Piso" → 1..10 para todas las torres.</li>
 *   <li><b>Excepción contextual</b> ({@code parentValorId} apuntando a un valor
 *       padre concreto): sobreescribe la plantilla en esa rama. Ej: bajo
 *       Torre "B", el tipo "Piso" solo permite 1..5.</li>
 * </ul>
 *
 * <p>Resolución: si existen valores contextuales para (tipo + valor padre) se
 * usan esos; en caso contrario cae a la plantilla global del tipo.</p>
 */
@Entity
@Table(name = "valores_tipo_propiedad")
public class ValorTipoPropiedad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tipo_id", nullable = false)
    private Long tipoId;

    @Column(nullable = false, length = 50)
    private String valor;

    /** null → plantilla global del tipo. No null → excepción bajo ese valor padre. */
    @Column(name = "parent_valor_id")
    private Long parentValorId;

    @Column(nullable = false)
    private int orden = 0;

    @Column(nullable = false)
    private boolean activo = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTipoId() { return tipoId; }
    public void setTipoId(Long tipoId) { this.tipoId = tipoId; }

    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }

    public Long getParentValorId() { return parentValorId; }
    public void setParentValorId(Long parentValorId) { this.parentValorId = parentValorId; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
}
