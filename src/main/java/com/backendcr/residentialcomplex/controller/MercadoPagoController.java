package com.backendcr.residentialcomplex.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoints GET de retorno de MercadoPago (back_urls).
 *
 * Estos paths son interceptados por el WebView de Flutter antes de que MP
 * los llegue a abrir como páginas reales. Se mantienen como landing pages
 * válidas para el caso de sandbox en browser o redirect no interceptado.
 *
 * Los endpoints de checkout, webhook y confirmación están en PasarelaController.
 */
@Slf4j
@RestController
public class MercadoPagoController {

    @GetMapping("/api/mp/pago-exito")
    public ResponseEntity<Map<String, String>> pagoExito(@RequestParam Map<String, String> params) {
        log.info("MP back_url éxito - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "aprobado", "mensaje", "Pago procesado correctamente"));
    }

    @GetMapping("/api/mp/pago-fallo")
    public ResponseEntity<Map<String, String>> pagoFallo(@RequestParam Map<String, String> params) {
        log.info("MP back_url fallo - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "fallido", "mensaje", "El pago no pudo procesarse"));
    }

    @GetMapping("/api/mp/pago-pendiente")
    public ResponseEntity<Map<String, String>> pagoPendiente(@RequestParam Map<String, String> params) {
        log.info("MP back_url pendiente - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "pendiente", "mensaje", "El pago está en revisión"));
    }
}
