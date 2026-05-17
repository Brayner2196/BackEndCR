package com.backendcr.residentialcomplex.dto.usuario;

import java.time.format.DateTimeFormatter;

import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.EstadoUsuario;

public record UsuarioResponse(
        Long id,
        String nombre,
        String email,
        String rol,
        String telefono,
        EstadoUsuario estado,
        String creadoEn
) {
	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public static UsuarioResponse from(Usuario usuario, String email, String rol) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                email,
                rol,
                usuario.getTelefono(),
                usuario.getEstado(),
                usuario.getCreadoEn().format(FORMATTER)
        );
    }
}
