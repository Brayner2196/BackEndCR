package com.backendcr.residentialcomplex.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

@Service
public class UsuarioService {

	private final UsuarioRepository usuarioRepository;

	public UsuarioService(UsuarioRepository usuarioRepository) {
		this.usuarioRepository = usuarioRepository;
	}

	public List<Usuario> listarTodos() {
		return usuarioRepository.findAll();
	}

	public Usuario guardar(Usuario usuario) {
		if (usuarioRepository.existsByEmail(usuario.getEmail())) {
			throw new RuntimeException("Ya existe un usuario con ese email en este tenant");
		}
		return usuarioRepository.save(usuario);
	}

}
