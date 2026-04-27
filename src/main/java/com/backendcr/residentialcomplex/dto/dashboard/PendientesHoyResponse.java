package com.backendcr.residentialcomplex.dto.dashboard;

public record PendientesHoyResponse(
        long comprobantes,
        long pqrs,
        long reservas,
        long total
) {}
