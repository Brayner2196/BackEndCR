package com.backendcr.residentialcomplex.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * Configuración del cliente S3 para el almacenamiento de archivos.
 *
 * Compatible con Railway Buckets o cualquier storage S3-compatible (MinIO, AWS S3, etc.).
 * Las credenciales y el endpoint se inyectan por variables de entorno (nunca hardcodeadas).
 *
 * En entorno local, si {@code bucket.endpoint} queda vacío el cliente se construye
 * apuntando a AWS por defecto pero sin usarse, de modo que la app arranca sin bucket configurado.
 */
@Configuration
public class S3Config {

	@Value("${bucket.endpoint:}")
	private String endpoint;

	@Value("${bucket.region:us-east-1}")
	private String region;

	@Value("${bucket.access-key:}")
	private String accessKey;

	@Value("${bucket.secret-key:}")
	private String secretKey;

	@Bean
	public S3Client s3Client() {
		S3Client.Builder builder = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey)))
				// Path-style: requerido por la mayoría de storages S3-compatible (Railway/MinIO).
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build());

		// Solo sobreescribimos el endpoint si está configurado (evita romper el arranque local).
		if (endpoint != null && !endpoint.isBlank()) {
			builder.endpointOverride(URI.create(endpoint));
		}

		return builder.build();
	}
}
