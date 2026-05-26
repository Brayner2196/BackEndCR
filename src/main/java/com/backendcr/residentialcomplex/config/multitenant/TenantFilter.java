package com.backendcr.residentialcomplex.config.multitenant;

import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TenantFilter extends OncePerRequestFilter {

	private final TenantRepository tenantRepository;

	/** Caché schemaName → timezone para evitar un SELECT en cada request. */
	private final ConcurrentHashMap<String, String> timezoneCache = new ConcurrentHashMap<>();

	public TenantFilter(TenantRepository tenantRepository) {
		this.tenantRepository = tenantRepository;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/auth")
                || path.startsWith("/api/tenants")
                || path.startsWith("/api/pago/webhook/")   // webhooks Wompi, Bold, MP (sin tenant header)
                || path.equals("/api/mp/webhook")
                || path.startsWith("/api/mp/confirmar/")
                || path.startsWith("/api/mp/pago-");
    }

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String tenant = request.getHeader("X-Tenant-ID");

		if (tenant == null || tenant.isBlank()) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Falta el header X-Tenant-ID");
			return;
		}
		if (!tenant.matches("^[a-zA-Z0-9_]+$")) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Tenant inválido");
			return;
		}

		// Resolución de timezone con caché (miss → consulta BD una sola vez por schema)
		String timezone = timezoneCache.computeIfAbsent(tenant, schema ->
				tenantRepository.findBySchemaName(schema)
						.map(t -> t.getTimezone() != null ? t.getTimezone() : "America/Bogota")
						.orElse("America/Bogota")
		);

		TenantContext.setTenant(tenant);
		TenantContext.setTimezone(timezone);

		try {
			filterChain.doFilter(request, response);
		} finally {
			TenantContext.clear();
		}
	}
}
