package com.backendcr.residentialcomplex.dto.usuario;

import java.time.Instant;

import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        String telefono,
        EstadoUsuario estado,
        boolean activo,
        Instant creadoEn
) {
    public static UsuarioResponse from(Usuario usuario, String email, String rol, boolean activo) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                email,
                rol,
                usuario.getTelefono(),
                usuario.getEstado(),
                activo,
                usuario.getCreadoEn()
        );
    }
}
