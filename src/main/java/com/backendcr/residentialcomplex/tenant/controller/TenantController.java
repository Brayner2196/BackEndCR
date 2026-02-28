package com.backendcr.residentialcomplex.tenant.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.backendcr.residentialcomplex.entity.Tenant;
import com.backendcr.residentialcomplex.tenant.dto.CrearTenantRequest;
import com.backendcr.residentialcomplex.tenant.service.TenantService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

	private final TenantService tenantService;

	@PostMapping
	public ResponseEntity<Tenant> crearTenant(@RequestBody CrearTenantRequest request) {
		Tenant tenant = tenantService.crearTenant(request);
		return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
	}

	@GetMapping
	public ResponseEntity<List<Tenant>> obtenerTenants() {
		List<Tenant> tenants = tenantService.obtenerTenants();
		return ResponseEntity.ok(tenants);
	}
}
