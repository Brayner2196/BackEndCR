package com.backendcr.residentialcomplex.entity;

import com.backendcr.residentialcomplex.config.EncryptedStringConverter;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import jakarta.persistence.*;
import java.time.Instant;

/**
 * Configuración de una pasarela de pago para un tenant específico.
 * Vive en el schema PUBLIC (igual que Tenant) para que sea accesible
 * desde los webhooks sin necesidad de conocer el schema del tenant.
 *
 * Un tenant puede tener N pasarelas activas con distinta prioridad.
 */
@Entity
@Table(
    name = "tenant_pasarelas",
    schema = "public",
    uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "tipo_pasarela"})
)
public class TenantPasarela {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK al tenant dueño de esta configuración */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pasarela", nullable = false)
    private TipoPasarela tipoPasarela;

    /** Si esta pasarela está habilitada para el tenant */
    @Column(nullable = false)
    private boolean activa = true;

    /**
     * Orden de aparición cuando el tenant tiene varias pasarelas.
     * Menor número = aparece primero en la app.
     */
    @Column(nullable = false)
    private int prioridad = 1;

    /** Modo sandbox/pruebas. true = no cobra real. */
    @Column(nullable = false)
    private boolean sandbox = false;

    // ── Credenciales comunes a todas las pasarelas ─────────────────────────
    // Para MP: accessToken = access_token, publicKey = public_key de prueba
    // Para Wompi: publicKey = llave pública, privateKey = llave privada
    // Para Bold: publicKey = api_key pública, privateKey = api_key privada

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "public_key")
    private String publicKey;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "private_key")
    private String privateKey;

    /** Secret para verificar firma de webhooks */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "webhook_secret")
    private String webhookSecret;

    // ── Auditoría ──────────────────────────────────────────────────────────────
    /** Fecha de creación del registro */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Última vez que se modificó la configuración */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /** Email del admin que hizo el último cambio */
    @Column(name = "updated_by")
    private String updatedBy;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ── URL de retorno override (si null usa las globales de application.properties) ──
    @Column(name = "success_url")
    private String successUrl;

    @Column(name = "failure_url")
    private String failureUrl;

    @Column(name = "pending_url")
    private String pendingUrl;

    // ── Getters y Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }

    public TipoPasarela getTipoPasarela() { return tipoPasarela; }
    public void setTipoPasarela(TipoPasarela tipoPasarela) { this.tipoPasarela = tipoPasarela; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    public int getPrioridad() { return prioridad; }
    public void setPrioridad(int prioridad) { this.prioridad = prioridad; }

    public boolean isSandbox() { return sandbox; }
    public void setSandbox(boolean sandbox) { this.sandbox = sandbox; }

    public String getPublicKey() { return publicKey; }
    public void setPublicKey(String publicKey) { this.publicKey = publicKey; }

    public String getPrivateKey() { return privateKey; }
    public void setPrivateKey(String privateKey) { this.privateKey = privateKey; }

    public String getWebhookSecret() { return webhookSecret; }
    public void setWebhookSecret(String webhookSecret) { this.webhookSecret = webhookSecret; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }

    public String getFailureUrl() { return failureUrl; }
    public void setFailureUrl(String failureUrl) { this.failureUrl = failureUrl; }

    public String getPendingUrl() { return pendingUrl; }
    public void setPendingUrl(String pendingUrl) { this.pendingUrl = pendingUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }

    /**
     * Nunca exponer credenciales en logs. Solo datos no sensibles.
     */
    @Override
    public String toString() {
        return "TenantPasarela{" +
               "id=" + id +
               ", tenantId=" + (tenant != null ? tenant.getId() : null) +
               ", tipo=" + tipoPasarela +
               ", activa=" + activa +
               ", sandbox=" + sandbox +
               ", prioridad=" + prioridad +
               '}';
        // privateKey, publicKey, webhookSecret OMITIDOS intencionalmente
    }
}
