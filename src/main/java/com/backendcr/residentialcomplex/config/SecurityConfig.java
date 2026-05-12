package com.backendcr.residentialcomplex.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
					.requestMatchers("/auth/login","/auth/login/seleccionar", "/auth/registro", "/auth/tiposPropiedad").permitAll()
					.requestMatchers("/api/mp/webhook", "/api/mp/pago-exito", "/api/mp/pago-fallo", "/api/mp/pago-pendiente").permitAll()
					.requestMatchers("/api/tenants/**").hasRole("SUPER_ADMIN")
					.anyRequest().authenticated()
		).sessionManagement(session -> session
					.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
		  ) .addFilterBefore(JwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterAfter(tenantFilter, JwtAuthFilter.class);
		return http.build();
	}

}
