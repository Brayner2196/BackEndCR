package com.backendcr.residentialcomplex.config.multitenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;


@Component
public class TenantFilter implements Filter{

	@Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String tenant = req.getHeader("X-Tenant-ID");

        // Validación básica
        if (tenant == null || tenant.isBlank()) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Falta el header X-Tenant-ID");
            return;
        }
        // Solo letras, números y guiones bajos (evita inyección SQL en schema name)
        if (!tenant.matches("^[a-zA-Z0-9_]+$")) {
            res.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant inválido");
            return;
        }

        TenantContext.setTenant(tenant);

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear(); 
        }
    }

}
