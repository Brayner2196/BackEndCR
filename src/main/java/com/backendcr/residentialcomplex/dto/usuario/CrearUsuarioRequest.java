package com.backendcr.residentialcomplex.dto.usuario;

import java.util.List;

import com.backendcr.residentialcomplex.dto.propiedad.PropiedadPathItemDto;

import com.backendcr.residentialcomplex.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CrearUsuarioRequest(

        @NotBlank
        @Size(min = 3, message = "El nombre debe tener al menos 3 caracteres")
        String nombre,

        @NotBlank @Email
        String email,

        @NotBlank
        @ValidPassword
        String password,

        @NotBlank
        @Pattern(regexp = "TENANT_ADMIN|RESIDENTE|RESIDENTE_PENDIENTE|PROPIETARIO|INQUILINO|VIGILANTE|PORTERO|PISCINERO|CONTADOR",
                 message = "Rol no válido")
        String rol,

        String telefono,

        List<PropiedadPathItemDto> propiedadPath
) {}
