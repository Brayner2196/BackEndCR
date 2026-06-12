package com.backendcr.residentialcomplex.dto.pago;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado del preview de generación de cobros para un período.
 * No persiste ningún dato — es un dry-run informativo.
 */
public record CobroPreviewResponse(
        int anio,
        int mes,
        int totalPropiedades,
        int yaGenerados,
        int pendientesDeGenerar,
        BigDecimal montoTotalEstimado,
        List<DetalleGrupo> grupos,
        List<String> advertencias
) {
    public record DetalleGrupo(
            String nombreTipo,
            String periodicidad,
            int cantidad,
            BigDecimal montoPorUnidad,
            BigDecimal subtotal,
            List<PropiedadDetalle> propiedades
    ) {}

    /** Detalle por propiedad individual dentro de un grupo. */
    public record PropiedadDetalle(
            Long propiedadId,
            String pathTexto,
            BigDecimal monto
    ) {}
}
