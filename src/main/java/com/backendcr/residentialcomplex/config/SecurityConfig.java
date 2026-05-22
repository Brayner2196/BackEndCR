package com.backendcr.residentialcomplex.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.backendcr.residentialcomplex.auth.JwtAuthFilter;
import com.backendcr.residentialcomplex.config.multitenant.TenantFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
	
	private final TenantFilter tenantFilter;
	private final JwtAuthFilter JwtAuthFilter;
	
	@Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(csrf -> csrf.disable())
			.authorizeHttpRequests(auth -> auth
					.requestMatchers("/auth/login","/auth/login/seleccionar", "/auth/registro", "/auth/tiposPropiedad", "/auth/refresh").permitAll()
					// Webhooks pasarelas (sin auth — vienen de servicios externos)
					.requestMatchers(
						"/api/mp/webhook", "/api/mp/confirmar/**",
						"/api/mp/pago-exito", "/api/mp/pago-fallo", "/api/mp/pago-pendiente",
						"/api/pago/webhook/mp", "/api/pago/webhook/wompi", "/api/pago/webhook/bold",
						"/api/pago/confirmar/mp/**"
					).permitAll()
					.requestMatchers("/api/tenants/**").hasRole("SUPER_ADMIN")
					.requestMatchers("/api/super/**").hasRole("SUPER_ADMIN")
					.anyRequest().authenticated()
		).sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		).exceptionHandling(ex -> ex
				// Sin Bearer token → 401 (no 403 default de Spring Security)
				.authenticationEntryPoint((req, res, e) -> {
					res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
					res.setContentType(MediaType.APPLICATION_JSON_VALUE);
					res.getWriter().write("{\"message\":\"No autenticado\"}");
				})
				// Rol incorrecto → 403 con body JSON legible
				.accessDeniedHandler((req, res, e) -> {
					res.setStatus(HttpServletResponse.SC_FORBIDDEN);
					res.setContentType(MediaType.APPLICATION_JSON_VALUE);
					res.getWriter().write("{\"message\":\"Acceso denegado\"}");
				})
		).addFilterBefore(JwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
		 .addFilterAfter(tenantFilter, JwtAuthFilter.class);
		return http.build();
	}

}
