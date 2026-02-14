package com.backendcr.residentialcomplex.multitenancy;

import jakarta.persistence.EntityManager;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.stereotype.Component;

/**
 * Utility class to apply Hibernate filters for multitenant queries.
 */
@Component
public class TenantFilter {
    
    public static final String TENANT_FILTER = "tenantFilter";
    public static final String TENANT_PARAM = "tenantId";
    
    public void applyFilter(EntityManager entityManager) {
        String currentTenant = TenantContext.getCurrentTenant();
        if (currentTenant != null) {
            Session session = entityManager.unwrap(Session.class);
            Filter filter = session.enableFilter(TENANT_FILTER);
            filter.setParameter(TENANT_PARAM, currentTenant);
        }
    }
}
