package com.backendcr.residentialcomplex.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_tokens", 
		schema = "public",
		uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "plataforma"})
)
public class DeviceToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(nullable = false, length = 20)
    private String plataforma; // ANDROID, IOS, WEB

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    public DeviceToken() {}

    public DeviceToken(Long usuarioId, String tenantId, String token, String plataforma) {
        this.usuarioId = usuarioId;
        this.tenantId = tenantId;
        this.token = token;
        this.plataforma = plataforma;
    }

    public Long getId() { return id; }

    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
}
