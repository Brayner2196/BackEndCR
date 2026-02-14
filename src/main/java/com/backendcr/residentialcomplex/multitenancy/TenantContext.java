package com.backendcr.residentialcomplex.multitenancy;

/**
 * Thread-safe context holder for current tenant identifier.
 * Uses InheritableThreadLocal to propagate tenant context to child threads.
 */
public class TenantContext {
    
    private static final ThreadLocal<String> currentTenant = new InheritableThreadLocal<>();
    
    public static void setCurrentTenant(String tenantId) {
        currentTenant.set(tenantId);
    }
    
    public static String getCurrentTenant() {
        return currentTenant.get();
    }
    
    public static void clear() {
        currentTenant.remove();
    }
}
