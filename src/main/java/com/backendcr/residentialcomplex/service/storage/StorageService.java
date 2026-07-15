package com.backendcr.residentialcomplex.service.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstracción de almacenamiento de archivos.
 *
 * Mantiene el resto del código desacoplado del proveedor concreto (S3/Railway, disco, etc.):
 * los servicios de dominio (productos, actas, PQR...) dependen de esta interfaz, no de S3.
 * Para migrar de proveedor basta con otra implementación, sin tocar controllers ni entidades.
 *
 * Convención de "key": {modulo}/{tenant}/{uuid}.{ext} — así los archivos quedan aislados
 * por tenant, igual que los schemas de la base de datos.
 */
public interface StorageService {

	/**
	 * Sube un archivo y retorna su key (identificador único dentro del bucket).
	 *
	 * @param archivo archivo recibido (no nulo, no vacío)
	 * @param modulo  carpeta lógica de primer nivel (ej: "productos", "actas")
	 * @return key generada, que debe persistirse en la entidad correspondiente
	 */
	String subir(MultipartFile archivo, String modulo);

	/**
	 * Reemplaza un archivo existente: sube el nuevo y elimina el anterior.
	 *
	 * @param archivo    nuevo archivo
	 * @param modulo     carpeta lógica
	 * @param keyAnterior key del archivo a reemplazar (puede ser null si no existía)
	 * @return key del nuevo archivo
	 */
	String reemplazar(MultipartFile archivo, String modulo, String keyAnterior);

	/**
	 * Descarga el contenido binario de un archivo.
	 *
	 * @param key key del archivo
	 * @return bytes del archivo
	 */
	byte[] descargar(String key);

	/**
	 * Retorna el content-type almacenado del archivo (ej: "image/webp"),
	 * útil para servirlo por un endpoint. Retorna "application/octet-stream" si no se conoce.
	 */
	String obtenerContentType(String key);

	/**
	 * Elimina un archivo. No falla si la key es null/blank o el objeto ya no existe.
	 */
	void eliminar(String key);

	/**
	 * Indica si existe un objeto con esa key.
	 */
	boolean existe(String key);
}
