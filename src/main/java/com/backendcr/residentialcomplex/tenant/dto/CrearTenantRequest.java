package com.backendcr.residentialcomplex.tenant.dto;

import java.util.List;

import com.backendcr.residentialcomplex.dto.pasarela.PasarelaConfigRequest;
import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CrearTenantRequest(

        @NotBlank
        @Pattern(regexp = "^[a-z0-9_]+$",
                 message = "El schemaName solo puede contener letras minúsculas, números y guiones bajos")
        String schemaName,

        @NotBlank
        String nombre,

        @NotBlank
        String codigo,

        @NotBlank @Email
        String emailAdmin,

        @NotBlank
        String passwordAdmin,

        String direccion,

        /** Ej: "America/Bogota", "America/Argentina/Buenos_Aires". Opcional — default Colombia. */
        String timezone,

        List<TipoPropiedadNodoDto> tiposPropiedad,

        /**
         * Pasarelas de pago a configurar desde la creación del tenant.
         * Opcional — se pueden agregar o modificar después via /api/tenants/{id}/pasarelas
         */
        List<PasarelaConfigRequest> pasarelas
) {}
