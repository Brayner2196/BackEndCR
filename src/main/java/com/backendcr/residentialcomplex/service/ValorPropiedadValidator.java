package com.backendcr.residentialcomplex.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.dto.propiedad.PropiedadPathItemDto;
import com.backendcr.residentialcomplex.dto.propiedad.ValorTipoPropiedadDto;

import lombok.RequiredArgsConstructor;

/**
 * Validador reutilizable del path de una propiedad contra el catálogo de
 * valores permitidos ({@link ValorTipoPropiedadService}).
 *
 * <p>Recorre el path de arriba hacia abajo resolviendo, en cada nivel, los
 * valores permitidos según el valor padre seleccionado (modelo híbrido). Si un
 * valor no está en el catálogo permitido, lanza {@code 400}. Es la barrera que
 * impide que se creen propiedades con valores arbitrarios (ej. "ABCDERGRTGG"),
 * tanto desde la app como desde llamadas directas a la API.</p>
 *
 * <p><b>Retrocompatible:</b> si un tipo aún no tiene catálogo configurado, ese
 * nivel se acepta libre (no rompe tenants que todavía no definieron valores).</p>
 */
@Component
@RequiredArgsConstructor
public class ValorPropiedadValidator {

    private final ValorTipoPropiedadService valorService;

    public void validarPath(List<PropiedadPathItemDto> path) {
        if (path == null || path.isEmpty()) return;

        Long parentValorId = null;

        for (PropiedadPathItemDto item : path) {
            // Sin catálogo para este tipo → se permite valor libre (retrocompatible).
            if (!valorService.tipoTieneCatalogo(item.tipoId())) {
                parentValorId = null;
                continue;
            }

            String valorNorm = ValorTipoPropiedadService.normalizar(item.valor());
            List<ValorTipoPropiedadDto> permitidos =
                    valorService.resolverPermitidos(item.tipoId(), parentValorId);

            ValorTipoPropiedadDto match = permitidos.stream()
                    .filter(v -> ValorTipoPropiedadService.normalizar(v.valor()).equals(valorNorm))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "El valor '" + item.valor().trim() + "' no está permitido. "
                                    + "Selecciona un valor válido de la lista."));

            // El id del valor elegido es el padre para resolver el siguiente nivel.
            parentValorId = match.id();
        }
    }
}
