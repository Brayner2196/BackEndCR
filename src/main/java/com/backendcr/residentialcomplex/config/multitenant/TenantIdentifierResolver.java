package com.backendcr.residentialcomplex.config.multitenant;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<Object> {

	private static final String DEFAULT_TENANT = "public";

	@Override
	public String resolveCurrentTenantIdentifier() {
		String tenant = TenantContext.getTenant();
		return (tenant != null && !tenant.isBlank()) ? tenant : DEFAULT_TENANT;
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}

}
