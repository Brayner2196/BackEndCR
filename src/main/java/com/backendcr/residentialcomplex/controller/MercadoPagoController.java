package com.backendcr.residentialcomplex.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Landing pages GET de retorno para pasarelas de pago.
 *
 * Los paths /api/mp/* son específicos de MercadoPago (back_urls de preferencia).
 * El path /api/pago/exito es genérico y lo usan Wompi y Bold como redirect_url fallback.
 *
 * Todos son interceptados por el WebView de Flutter antes de que la pasarela
 * los abra como páginas reales. Se mantienen como endpoints válidos para
 * sandbox en browser o redirects no interceptados.
 *
 * Los endpoints de checkout, webhook y confirmación están en PasarelaController.
 */
@Slf4j
@RestController
public class MercadoPagoController {

    // ─── MercadoPago (back_urls específicos) ─────────────────────────────────

    @GetMapping("/api/mp/pago-exito")
    public ResponseEntity<Map<String, String>> mpPagoExito(@RequestParam Map<String, String> params) {
        log.info("MP back_url éxito - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "aprobado", "mensaje", "Pago procesado correctamente"));
    }

    @GetMapping("/api/mp/pago-fallo")
    public ResponseEntity<Map<String, String>> mpPagoFallo(@RequestParam Map<String, String> params) {
        log.info("MP back_url fallo - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "fallido", "mensaje", "El pago no pudo procesarse"));
    }

    @GetMapping("/api/mp/pago-pendiente")
    public ResponseEntity<Map<String, String>> mpPagoPendiente(@RequestParam Map<String, String> params) {
        log.info("MP back_url pendiente - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "pendiente", "mensaje", "El pago está en revisión"));
    }

    // ─── Genérico (Wompi, Bold) ───────────────────────────────────────────────

    /**
     * Endpoint de retorno genérico para pasarelas que no son MercadoPago.
     * El WebView de Flutter lo intercepta antes de que el navegador lo cargue.
     * El query param "ref" contiene tenantId__cobroId__usuarioId (Wompi).
     */
    @GetMapping("/api/pago/exito")
    public ResponseEntity<Map<String, String>> pagoExitoGenerico(@RequestParam Map<String, String> params) {
        log.info("Retorno de pago genérico - params={}", params);
        return ResponseEntity.ok(Map.of("estado", "aprobado", "mensaje", "Pago recibido correctamente"));
    }
}
