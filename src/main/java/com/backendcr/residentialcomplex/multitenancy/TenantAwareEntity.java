package com.backendcr.residentialcomplex.multitenancy;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;

/**
 * Base entity class for multitenant entities.
 * Automatically sets tenant_id before persist and update operations.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class TenantAwareEntity {
    
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;
    
    @PrePersist
    @PreUpdate
    private void setTenantId() {
        String currentTenant = TenantContext.getCurrentTenant();
        if (this.tenantId == null && currentTenant != null) {
            this.tenantId = currentTenant;
        }
    }
}
