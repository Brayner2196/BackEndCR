package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.ConfiguracionMora;
import com.backendcr.residentialcomplex.entity.enums.TipoCalculoMora;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ConfiguracionMoraResponse(
        Long id,
        TipoCalculoMora tipoCalculo,
        BigDecimal porcentajeMensual,
        BigDecimal montoFijo,
        int diasGracia,
        boolean activo,
        LocalDate fechaVigencia,
        LocalDateTime creadoEn
) {
    public static ConfiguracionMoraResponse from(ConfiguracionMora e) {
        return new ConfiguracionMoraResponse(
                e.getId(),
                e.getTipoCalculo(),
                e.getPorcentajeMensual(),
                e.getMontoFijo(),
                e.getDiasGracia(),
                e.isActivo(),
                e.getFechaVigencia(),
                e.getCreadoEn()
        );
    }
}
