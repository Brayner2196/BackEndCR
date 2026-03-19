package com.backendcr.residentialcomplex.dto.usuario;

import java.time.LocalDateTime;

import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        String apto,
        String torre,
        String telefono,
        EstadoUsuario estado,
        LocalDateTime creadoEn
) {
    public static UsuarioResponse from(Usuario usuario, String email, String rol) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                email,
                rol,
                usuario.getApto(),
                usuario.getTorre(),
                usuario.getTelefono(),
                usuario.getEstado(),
                usuario.getCreadoEn()
        );
    }
}
