package com.backendcr.residentialcomplex.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.backendcr.residentialcomplex.service.storage.StorageService;

import lombok.RequiredArgsConstructor;

/**
 * Controller DESECHABLE para probar el {@link StorageService} contra el bucket S3.
 *
 * <p>Requiere JWT válido + header {@code X-Tenant-ID} (como el resto de la API).
 * No tiene restricción de rol: sirve solo para verificar la integración con el bucket.
 * <b>Eliminar este controller una vez validada la subida de archivos.</b></p>
 */
@RestController
@RequestMapping("/api/test-storage")
@RequiredArgsConstructor
public class StorageController {

	private final StorageService storageService;

	/**
	 * Sube un archivo al bucket bajo el módulo "pruebas".
	 * multipart/form-data → campo "archivo" (tipo File).
	 *
	 * @return la key generada: pruebas/{tenant}/{uuid}.ext
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<String> subir(@RequestPart("archivo") MultipartFile archivo) {
		String key = storageService.subir(archivo, "pruebas");
		return ResponseEntity.ok(key);
	}

	/**
	 * Descarga un archivo por su key (para verificar que se guardó correctamente).
	 * Ej: GET /api/test-storage/descargar?key=pruebas/mi_tenant/uuid.png
	 */
	@GetMapping("/descargar")
	public ResponseEntity<byte[]> descargar(@RequestParam String key) {
		byte[] contenido = storageService.descargar(key);
		String contentType = storageService.obtenerContentType(key);
		return ResponseEntity.ok()
				.header("Content-Type", contentType)
				.body(contenido);
	}

	/**
	 * Elimina un archivo por su key.
	 * Ej: DELETE /api/test-storage?key=pruebas/mi_tenant/uuid.png
	 */
	@DeleteMapping
	public ResponseEntity<String> eliminar(@RequestParam String key) {
		storageService.eliminar(key);
		return ResponseEntity.ok("Eliminado (o inexistente): " + key);
	}
}
