package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.dto.conjunto.MiConjuntoRequest;
import com.backendcr.residentialcomplex.dto.conjunto.MiConjuntoResponse;
import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminConjuntoService {

    private final TenantRepository tenantRepository;

    public MiConjuntoResponse obtener() {
        Tenant tenant = resolverTenantActual();
        return toResponse(tenant);
    }

    @Transactional
    public MiConjuntoResponse actualizar(MiConjuntoRequest req) {
        Tenant tenant = resolverTenantActual();
        tenant.setNombre(req.nombre().trim());
        if (req.direccion() != null) {
            tenant.setDireccion(req.direccion().trim());
        }
        return toResponse(tenantRepository.save(tenant));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Tenant resolverTenantActual() {
        String schema = TenantContext.getTenant();
        return tenantRepository.findBySchemaName(schema)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Conjunto no encontrado"));
    }

    private MiConjuntoResponse toResponse(Tenant t) {
        return new MiConjuntoResponse(t.getNombre(), t.getCodigo(), t.getDireccion(), t.isActivo());
    }
}
