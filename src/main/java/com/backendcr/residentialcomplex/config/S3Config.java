package com.backendcr.residentialcomplex.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Configuración del acceso al almacenamiento de archivos.
 *
 * Apunta a Backblaze B2 vía su API S3-compatible (también sirve cualquier otro storage
 * S3-compatible: MinIO, AWS S3, etc.). Credenciales y endpoint se inyectan por variables
 * de entorno (nunca hardcodeadas).
 *
 * Expone dos beans:
 * <ul>
 *   <li>{@link S3Client}: operaciones de servidor (subir, borrar, metadata).</li>
 *   <li>{@link S3Presigner}: genera URLs firmadas para que el cliente descargue directo
 *       de B2, sin pasar los bytes por el backend.</li>
 * </ul>
 *
 * En entorno local, si {@code bucket.endpoint} queda vacío el cliente apunta a AWS por
 * defecto pero no se usa, de modo que la app arranca sin bucket configurado.
 */
@Configuration
public class S3Config {

	@Value("${bucket.endpoint:}")
	private String endpoint;

	@Value("${bucket.region:us-east-005}")
	private String region;

	@Value("${bucket.access-key:}")
	private String accessKey;

	@Value("${bucket.secret-key:}")
	private String secretKey;

	@Bean
	public S3Client s3Client() {
		S3ClientBuilder builder = S3Client.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey)))
				// Path-style: compatible con B2 y con la mayoría de storages S3-compatible.
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build());

		// Solo sobreescribimos el endpoint si está configurado (evita romper el arranque local).
		if (endpoint != null && !endpoint.isBlank()) {
			builder.endpointOverride(URI.create(endpoint));
		}

		return builder.build();
	}

	@Bean
	public S3Presigner s3Presigner() {
		S3Presigner.Builder builder = S3Presigner.builder()
				.region(Region.of(region))
				.credentialsProvider(StaticCredentialsProvider.create(
						AwsBasicCredentials.create(accessKey, secretKey)))
				// Debe coincidir con la config del S3Client para que la firma sea válida.
				.serviceConfiguration(S3Configuration.builder()
						.pathStyleAccessEnabled(true)
						.build());

		if (endpoint != null && !endpoint.isBlank()) {
			builder.endpointOverride(URI.create(endpoint));
		}

		return builder.build();
	}
}
