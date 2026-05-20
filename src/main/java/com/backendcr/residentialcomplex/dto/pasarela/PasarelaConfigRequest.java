package com.backendcr.residentialcomplex.dto.pasarela;

import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;
import jakarta.validation.constraints.NotNull;

/**
 * Request para crear o actualizar la configuración de una pasarela para un tenant.
 */
public record PasarelaConfigRequest(

        @NotNull
        TipoPasarela tipoPasarela,

        /** Clave pública / access token según la pasarela */
        @NotNull
        String publicKey,

        /** Clave privada / secret key según la pasarela */
        @NotNull
        String privateKey,

        /** Secret para validar firma de webhooks */
        String webhookSecret,

        /** true = modo sandbox/pruebas */
        boolean sandbox,

        /** Prioridad de aparición en la app (1 = primera). Default 1. */
        Integer prioridad,

        /** URL de retorno override para éxito (si null usa global) */
        String successUrl,

        /** URL de retorno override para fallo */
        String failureUrl,

        /** URL de retorno override para pendiente */
        String pendingUrl
) {}
