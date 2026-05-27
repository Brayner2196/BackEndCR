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
					// Webhooks y landing pages de pasarelas (sin auth — vienen de servicios externos o del WebView)
					.requestMatchers(
						// Landing pages MP (back_urls interceptadas por el WebView)
						"/api/mp/pago-exito", "/api/mp/pago-fallo", "/api/mp/pago-pendiente",
						// Landing page genérica (redirect_url de Wompi/Bold interceptada por el WebView)
						// DEBE ser pública: el WebView la carga sin Bearer token si onNavigationRequest no intercepta
						"/api/pago/exito",
						// Webhooks MP (por-tenant y global, sin auth — vienen de servidores de MP)
						"/api/pago/webhook/mp", "/api/pago/webhook/mp/**",
						// Webhooks Wompi y Bold
						"/api/pago/webhook/wompi", "/api/pago/webhook/bold",
						// Confirmación rápida MP desde la app (sin auth — se valida vía paymentId en MP)
						"/api/mp/webhook", "/api/mp/confirmar/**",
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
