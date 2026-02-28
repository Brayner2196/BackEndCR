package com.backendcr.residentialcomplex.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.service.UsuarioService;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

	private final UsuarioService usuarioService;

	public UsuarioController(UsuarioService usuarioService) {
		this.usuarioService = usuarioService;
	}

	// Header X-Tenant-ID lo maneja el filtro — el controller no sabe nada de
	// tenants
	@GetMapping
	public List<Usuario> listar() {
		return usuarioService.listarTodos();
	}

	@PostMapping
	public Usuario crear(@RequestBody Usuario usuario) {
		return usuarioService.guardar(usuario);
	}
}
