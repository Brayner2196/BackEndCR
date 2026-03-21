package com.backendcr.residentialcomplex.tenant.dto;

public record TenantResponse(
        Long id,
        String schemaName,
        String nombre,
        String codigo,
        boolean activo,
        String direccion
) {}
