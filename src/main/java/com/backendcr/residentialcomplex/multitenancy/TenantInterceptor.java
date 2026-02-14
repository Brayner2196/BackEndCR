package com.backendcr.residentialcomplex.multitenancy;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to extract tenant identifier from HTTP headers.
 * Expected header: X-Tenant-ID
 */
@Component
public class TenantInterceptor implements HandlerInterceptor {
    
    private static final String TENANT_HEADER = "X-Tenant-ID";
    private static final String DEFAULT_TENANT = "default";
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String tenantId = request.getHeader(TENANT_HEADER);
        
        // If no tenant header provided, use default tenant
        if (tenantId == null || tenantId.trim().isEmpty()) {
            tenantId = DEFAULT_TENANT;
        }
        
        TenantContext.setCurrentTenant(tenantId);
        return true;
    }
    
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
