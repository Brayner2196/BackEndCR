package com.backendcr.residentialcomplex.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Landing pages GET de retorno para pasarelas de pago.
 *
 * ESTRATEGIA: en vez de devolver JSON (que el WebView de Flutter mostraría
 * cuando onNavigationRequest no alcanza a interceptar el redirect), estos
 * endpoints devuelven una página HTML mínima que ejecuta inmediatamente un
 * JavaScript redirect al deep-link conjuntosapp://.
 *
 * Flujo garantizado:
 *  1. Pasarela redirige WebView a https://…/api/mp/pago-exito?payment_id=X…
 *  2. Si onNavigationRequest interceptó antes → NavigationDecision.prevent → OK.
 *  3. Si no interceptó (Android HTTP-302 bypass) → WebView carga la página.
 *  4. El JS de la página ejecuta: window.location = 'conjuntosapp://pago/exito?payment_id=X…'
 *  5. onNavigationRequest SÍ dispara para redirects JS → NavigationDecision.prevent → OK.
 *  6. Flutter extrae los params y confirma el pago.
 *
 * Los navegations iniciados por JavaScript SIEMPRE disparan onNavigationRequest.
 * Los deep-links (conjuntosapp://) NUNCA se pueden cargar en el WebView,
 * así que prevent es siempre efectivo.
 */
@Slf4j
@RestController
public class MercadoPagoController {

    private static final String APP_SCHEME = "conjuntosapp";

    // ─── MercadoPago (back_urls específicos) ─────────────────────────────────

    @GetMapping("/api/mp/pago-exito")
    public ResponseEntity<String> mpPagoExito(@RequestParam Map<String, String> params) {
        log.info("MP back_url éxito - params={}", params);
        return deepLinkRedirect(APP_SCHEME + "://pago/exito", params, "Pago procesado");
    }

    @GetMapping("/api/mp/pago-fallo")
    public ResponseEntity<String> mpPagoFallo(@RequestParam Map<String, String> params) {
        log.info("MP back_url fallo - params={}", params);
        return deepLinkRedirect(APP_SCHEME + "://pago/fallo", params, "Pago fallido");
    }

    @GetMapping("/api/mp/pago-pendiente")
    public ResponseEntity<String> mpPagoPendiente(@RequestParam Map<String, String> params) {
        log.info("MP back_url pendiente - params={}", params);
        return deepLinkRedirect(APP_SCHEME + "://pago/pendiente", params, "Pago pendiente");
    }

    // ─── Genérico (Wompi, Bold) ───────────────────────────────────────────────

    /**
     * Endpoint de retorno genérico para Wompi y Bold.
     * Recibe ?ref=tenantId__cobroId__usuarioId&id=TX_ID (Wompi) y redirige vía JS
     * al deep-link que Flutter intercepta en onNavigationRequest.
     */
    @GetMapping("/api/pago/exito")
    public ResponseEntity<String> pagoExitoGenerico(@RequestParam Map<String, String> params) {
        log.info("Retorno de pago genérico (Wompi/Bold) - params={}", params);
        return deepLinkRedirect(APP_SCHEME + "://pago/exito", params, "Pago recibido");
    }

    // ─── Helper ──────────────────────────────────────────────────────────────

    /**
     * Devuelve una página HTML mínima con JavaScript que redirige inmediatamente
     * al deep-link de la app, preservando todos los query params de la pasarela.
     *
     * El <meta http-equiv="refresh"> es el fallback para contextos sin JS.
     * El JavaScript es el mecanismo principal: dispara onNavigationRequest en Flutter.
     */
    private ResponseEntity<String> deepLinkRedirect(
            String baseDeepLink, Map<String, String> params, String titulo) {

        String queryString = params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));

        String deepLink = queryString.isEmpty()
                ? baseDeepLink
                : baseDeepLink + "?" + queryString;

        // Escapar para HTML (los params pueden tener caracteres especiales)
        String deepLinkHtml = deepLink.replace("&", "&amp;").replace("\"", "&quot;");

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <meta http-equiv="refresh" content="0; url=%s">
                  <title>%s</title>
                </head>
                <body>
                  <p>Redirigiendo...</p>
                  <script>
                    // Este redirect JS dispara onNavigationRequest en Flutter WebView,
                    // que intercepta el deep-link y procesa el pago automáticamente.
                    window.location.replace('%s');
                  </script>
                </body>
                </html>
                """.formatted(deepLinkHtml, titulo, deepLink.replace("'", "\\'"));

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
