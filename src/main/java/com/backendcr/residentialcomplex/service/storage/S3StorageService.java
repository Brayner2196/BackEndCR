package com.backendcr.residentialcomplex.service.storage;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Implementación de {@link StorageService} sobre Backblaze B2 (API S3-compatible).
 *
 * Genera keys aisladas por tenant: {modulo}/{tenant}/{uuid}.{ext}. No guarda el nombre original
 * del archivo en la key (evita colisiones y caracteres inseguros); el content-type se conserva
 * como metadato del objeto. Las descargas para el cliente se sirven con URLs firmadas
 * (presigned), de modo que los bytes viajan directo de B2 al dispositivo, no por el backend.
 */
@Service
@RequiredArgsConstructor
public class S3StorageService implements StorageService {

	private static final Logger log = LoggerFactory.getLogger(S3StorageService.class);

	private static final String CONTENT_TYPE_POR_DEFECTO = "application/octet-stream";

	private final S3Client s3Client;
	private final S3Presigner s3Presigner;

	@Value("${bucket.name:}")
	private String bucketName;

	@Value("${bucket.url-ttl-minutes:15}")
	private long urlTtlMinutes;

	@Override
	public String subir(MultipartFile archivo, String modulo) {
		if (archivo == null || archivo.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo es obligatorio");
		}

		String key = construirKey(modulo, extraerExtension(archivo.getOriginalFilename()));
		String contentType = archivo.getContentType() != null ? archivo.getContentType() : CONTENT_TYPE_POR_DEFECTO;

		try {
			PutObjectRequest request = PutObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.contentType(contentType)
					.build();

			s3Client.putObject(request, RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize()));
			log.info("Archivo subido a B2: {}", key);
			return key;
		} catch (IOException e) {
			log.error("Error leyendo el archivo a subir ({}): {}", key, e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el archivo");
		} catch (S3Exception e) {
			log.error("Error subiendo a B2 ({}): {}", key, e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo almacenar el archivo");
		}
	}

	@Override
	public String reemplazar(MultipartFile archivo, String modulo, String keyAnterior) {
		String nuevaKey = subir(archivo, modulo);
		// Se elimina el anterior solo tras subir el nuevo con éxito (evita perder el archivo).
		eliminar(keyAnterior);
		return nuevaKey;
	}

	@Override
	public String generarUrlDescarga(String key) {
		if (key == null || key.isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La key del archivo es obligatoria");
		}
		try {
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();

			GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
					.signatureDuration(Duration.ofMinutes(urlTtlMinutes))
					.getObjectRequest(getObjectRequest)
					.build();

			PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
			return presigned.url().toString();
		} catch (S3Exception e) {
			log.error("Error generando URL firmada ({}): {}", key, e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo generar el enlace de descarga");
		}
	}

	@Override
	public byte[] descargar(String key) {
		try {
			GetObjectRequest request = GetObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();
			ResponseBytes<GetObjectResponse> objeto = s3Client.getObjectAsBytes(request);
			return objeto.asByteArray();
		} catch (NoSuchKeyException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
		} catch (S3Exception e) {
			log.error("Error descargando de B2 ({}): {}", key, e.getMessage());
			throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "No se pudo obtener el archivo");
		}
	}

	@Override
	public String obtenerContentType(String key) {
		try {
			HeadObjectRequest request = HeadObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();
			String contentType = s3Client.headObject(request).contentType();
			return contentType != null ? contentType : CONTENT_TYPE_POR_DEFECTO;
		} catch (NoSuchKeyException e) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Archivo no encontrado");
		} catch (S3Exception e) {
			log.error("Error consultando metadata en B2 ({}): {}", key, e.getMessage());
			return CONTENT_TYPE_POR_DEFECTO;
		}
	}

	@Override
	public void eliminar(String key) {
		if (key == null || key.isBlank()) {
			return;
		}
		try {
			DeleteObjectRequest request = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(key)
					.build();
			s3Client.deleteObject(request);
			log.info("Archivo eliminado de B2: {}", key);
		} catch (S3Exception e) {
			// No propagamos: borrar es best-effort (igual que el borrado silencioso de audios de acta).
			log.warn("No se pudo eliminar de B2 ({}): {}", key, e.getMessage());
		}
	}

	@Override
	public boolean existe(String key) {
		if (key == null || key.isBlank()) {
			return false;
		}
		try {
			s3Client.headObject(HeadObjectRequest.builder().bucket(bucketName).key(key).build());
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		} catch (S3Exception e) {
			log.warn("Error verificando existencia en B2 ({}): {}", key, e.getMessage());
			return false;
		}
	}

	// ─── Helpers ─────────────────────────────────────────────────────────────

	/** Construye la key aislada por tenant: {modulo}/{tenant}/{uuid}.{ext}. */
	private String construirKey(String modulo, String extension) {
		String tenant = TenantContext.getTenant();
		String carpetaTenant = (tenant != null && !tenant.isBlank()) ? tenant : "desconocido";
		String nombre = UUID.randomUUID().toString();
		if (extension != null && !extension.isBlank()) {
			nombre = nombre + "." + extension;
		}
		return modulo + "/" + carpetaTenant + "/" + nombre;
	}

	/** Extrae la extensión (en minúsculas, sin punto) del nombre original, o null si no tiene. */
	private String extraerExtension(String nombreOriginal) {
		if (nombreOriginal == null) {
			return null;
		}
		int punto = nombreOriginal.lastIndexOf('.');
		if (punto < 0 || punto == nombreOriginal.length() - 1) {
			return null;
		}
		return nombreOriginal.substring(punto + 1).toLowerCase();
	}
}
