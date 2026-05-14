package com.backendcr.residentialcomplex.dto.pago;

import com.backendcr.residentialcomplex.entity.enums.ConceptoCobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import java.math.BigDecimal;
import java.time.LocalDate;

public record CobroResponse(
        Long id,
        Long periodoId,
        Integer anio,
        Integer mes,
        Long propiedadId,
        String propiedadIdentificador,
        Long usuarioId,
        String usuarioNombre,
        ConceptoCobro concepto,
        String descripcion,
        BigDecimal montoBase,
        BigDecimal montoMora,
        BigDecimal montoTotal,
        BigDecimal montoPagado,
        BigDecimal montoPendiente,
        LocalDate fechaGeneracion,
        LocalDate fechaLimitePago,
        EstadoCobro estado,
        boolean tieneMovimientos
) {}
