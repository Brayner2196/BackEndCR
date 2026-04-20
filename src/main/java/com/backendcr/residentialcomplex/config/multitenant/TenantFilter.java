package com.backendcr.residentialcomplex.config.multitenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {
	
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth") || path.startsWith("/api/tenants");  // saltar filtro en rutas públicas
    }

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		
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
					filterChain.doFilter(request, response);
				} finally {
					TenantContext.clear();
				}	
	}

}
