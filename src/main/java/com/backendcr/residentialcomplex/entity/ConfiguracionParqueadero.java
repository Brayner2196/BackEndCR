package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_parqueadero")
public class ConfiguracionParqueadero {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_parqueaderos", nullable = false)
    private int totalParqueaderos = 0;

    @Column(name = "parqueaderos_comunes", nullable = false)
    private int parqueaderosComunes = 0;

    @Column(name = "parqueaderos_privados", nullable = false)
    private int parqueaderosPrivados = 0;

    @Column(name = "max_vehiculos_por_propiedad", nullable = false)
    private int maxVehiculosPorPropiedad = 2;

    // Flags de tipos permitidos
    @Column(name = "permite_carro", nullable = false)
    private boolean permiteCarro = true;

    @Column(name = "permite_moto", nullable = false)
    private boolean permiteMoto = true;

    @Column(name = "permite_bicicleta", nullable = false)
    private boolean permiteBicicleta = true;

    @Column(name = "requiere_aprobacion_vehiculo", nullable = false)
    private boolean requiereAprobacionVehiculo = false;

    @UpdateTimestamp
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    // ── Getters / Setters ─────────────────────────────────────

    public Long getId() { return id; }

    public int getTotalParqueaderos() { return totalParqueaderos; }
    public void setTotalParqueaderos(int totalParqueaderos) { this.totalParqueaderos = totalParqueaderos; }

    public int getParqueaderosComunes() { return parqueaderosComunes; }
    public void setParqueaderosComunes(int parqueaderosComunes) { this.parqueaderosComunes = parqueaderosComunes; }

    public int getParqueaderosPrivados() { return parqueaderosPrivados; }
    public void setParqueaderosPrivados(int parqueaderosPrivados) { this.parqueaderosPrivados = parqueaderosPrivados; }

    public int getMaxVehiculosPorPropiedad() { return maxVehiculosPorPropiedad; }
    public void setMaxVehiculosPorPropiedad(int maxVehiculosPorPropiedad) { this.maxVehiculosPorPropiedad = maxVehiculosPorPropiedad; }

    public boolean isPermiteCarro() { return permiteCarro; }
    public void setPermiteCarro(boolean permiteCarro) { this.permiteCarro = permiteCarro; }

    public boolean isPermiteMoto() { return permiteMoto; }
    public void setPermiteMoto(boolean permiteMoto) { this.permiteMoto = permiteMoto; }

    public boolean isPermiteBicicleta() { return permiteBicicleta; }
    public void setPermiteBicicleta(boolean permiteBicicleta) { this.permiteBicicleta = permiteBicicleta; }

    public boolean isRequiereAprobacionVehiculo() { return requiereAprobacionVehiculo; }
    public void setRequiereAprobacionVehiculo(boolean requiereAprobacionVehiculo) { this.requiereAprobacionVehiculo = requiereAprobacionVehiculo; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
