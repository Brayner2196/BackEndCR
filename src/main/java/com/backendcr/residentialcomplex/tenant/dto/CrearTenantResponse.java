package com.backendcr.residentialcomplex.tenant.dto;

public record CrearTenantResponse(
        Long tenantId,
        String schemaName,
        String nombre,
        String codigo,
        AdminInfo admin
) {
    public record AdminInfo(
            Long identidadId,
            String email
    ) {}
}
