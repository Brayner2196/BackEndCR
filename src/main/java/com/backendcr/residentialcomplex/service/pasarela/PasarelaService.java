package com.backendcr.residentialcomplex.service.pasarela;

import com.backendcr.residentialcomplex.dto.pasarela.CheckoutResponse;
import com.backendcr.residentialcomplex.entity.TenantPasarela;
import com.backendcr.residentialcomplex.entity.enums.TipoPasarela;

import java.math.BigDecimal;

/**
 * Contrato que deben cumplir todas las pasarelas de pago.
 * Cada implementación recibe la configuración del tenant via TenantPasarela
 * para no depender de variables globales de ambiente.
 */
public interface PasarelaService {

    /** Tipo de pasarela que implementa esta clase */
    TipoPasarela getTipo();

    /**
     * Crea un checkout URL para que el residente pague.
     *
     * @param config      credenciales y configuración del tenant para esta pasarela
     * @param cobroId     ID del cobro a pagar (en el schema del tenant)
     * @param usuarioId   ID del usuario que paga
     * @param tenantId    schemaName del tenant
     * @param monto       monto a cobrar (si null se usa el pendiente del cobro)
     * @return CheckoutResponse con la URL y el tipo de pasarela
     */
    CheckoutResponse crearCheckout(
            TenantPasarela config,
            Long cobroId,
            Long usuarioId,
            String tenantId,
            BigDecimal monto
    );

    /**
     * Procesa la notificación de pago de la pasarela.
     * Cada implementación extrae y valida los datos de su propio formato.
     *
     * @param config      credenciales del tenant (para validar firma, etc.)
     * @param payload     cuerpo crudo del webhook como String JSON
     * @param signature   header de firma (puede ser null si la pasarela no la usa)
     */
    void procesarWebhook(TenantPasarela config, String payload, String signature);
}
