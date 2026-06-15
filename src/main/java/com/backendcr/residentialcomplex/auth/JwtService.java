package com.backendcr.residentialcomplex.auth;

import java.security.Key;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	@Value("${jwt.secret}")
	private String secret;

	@Value("${jwt.expiration-ms}")
	private long expirationMs;


	private Key getKey() {
		return Keys.hmacShaKeyFor(secret.getBytes());
	}


	public String generarToken(Long id, String email, String rol, String tenantId) {
		return generarToken(id, email, rol, tenantId, false, null);
	}


	public String generarToken(Long id, String email, String rol, String tenantId,
			boolean esConsejero, String cargoConsejo) {
		var claims = new java.util.HashMap<String, Object>();
		claims.put("id", id);
		claims.put("rol", rol);
		claims.put("tenantId", tenantId);
		claims.put("esConsejero", esConsejero);
		if (cargoConsejo != null) {
			claims.put("cargoConsejo", cargoConsejo);
		}
		return Jwts.builder()
				.subject(email)
				.claims(claims)
				.issuedAt(new Date())
				.expiration(new Date(System.currentTimeMillis() + expirationMs))
				.signWith(getKey())
				.compact();
	}

	// Lee todos los claims del token
	public Claims extraerClaims(String token) {
		return Jwts.parser().verifyWith((javax.crypto.SecretKey) getKey()).build().parseSignedClaims(token)
				.getPayload();
	}

	public String extraerEmail(String token) {
		return extraerClaims(token).getSubject();
	}

	public String extraerRol(String token) {
		return extraerClaims(token).get("rol", String.class);
	}

	public String extraerTenantId(String token) {
		return extraerClaims(token).get("tenantId", String.class);
	}

	public Long extraerId(String token) {
		return extraerClaims(token).get("id", Long.class);
	}

	public boolean extraerEsConsejero(String token) {
		Boolean val = extraerClaims(token).get("esConsejero", Boolean.class);
		return Boolean.TRUE.equals(val);
	}

	public String extraerCargoConsejo(String token) {
		return extraerClaims(token).get("cargoConsejo", String.class);
	}

	public boolean tokenValido(String token) {
		try {
			extraerClaims(token); // si no lanza excepción, es válido
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			return false;
		}
	}
}
