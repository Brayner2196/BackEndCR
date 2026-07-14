package com.backendcr.residentialcomplex.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.dto.propiedad.ValorTipoPropiedadDto;
import com.backendcr.residentialcomplex.entity.ValorTipoPropiedad;
import com.backendcr.residentialcomplex.repository.TipoPropiedadRepository;
import com.backendcr.residentialcomplex.repository.ValorTipoPropiedadRepository;

import lombok.RequiredArgsConstructor;

/**
 * Gestión del catálogo de valores permitidos por tipo de propiedad y su
 * resolución híbrida (plantilla global + excepciones contextuales por rama).
 *
 * <p>La lógica de resolución vive aquí, separada, para poder reutilizarse desde
 * los controladores (dropdowns) y desde {@link ValorPropiedadValidator}.</p>
 */
@Service
@RequiredArgsConstructor
public class ValorTipoPropiedadService {

    private final ValorTipoPropiedadRepository valorRepo;
    private final TipoPropiedadRepository tipoRepo;

    // ── Resolución (para dropdowns y validación) ───────────────────────────────

    /**
     * Valores permitidos y activos para un nivel, dado el tipo y el valor padre
     * seleccionado (id de un {@link ValorTipoPropiedad}, o null para la raíz).
     *
     * <p>Modelo híbrido: primero busca excepciones contextuales bajo ese padre;
     * si no hay, cae a la plantilla global del tipo.</p>
     */
    public List<ValorTipoPropiedadDto> resolverPermitidos(Long tipoId, Long parentValorId) {
        List<ValorTipoPropiedad> resueltos;

        if (parentValorId != null) {
            resueltos = valorRepo
                    .findByTipoIdAndParentValorIdAndActivoTrueOrderByOrdenAscValorAsc(tipoId, parentValorId);
            if (resueltos.isEmpty()) {
                // Sin excepción para esta rama → plantilla global.
                resueltos = valorRepo
                        .findByTipoIdAndParentValorIdIsNullAndActivoTrueOrderByOrdenAscValorAsc(tipoId);
            }
        } else {
            resueltos = valorRepo
                    .findByTipoIdAndParentValorIdIsNullAndActivoTrueOrderByOrdenAscValorAsc(tipoId);
        }

        return resueltos.stream().map(this::toDto).toList();
    }

    /** ¿El tipo tiene algún valor activo definido? Si no, se permite valor libre (retrocompatible). */
    public boolean tipoTieneCatalogo(Long tipoId) {
        return valorRepo.existsByTipoIdAndActivoTrue(tipoId);
    }

    // ── Gestión (admin) ────────────────────────────────────────────────────────

    /** Todos los valores de un tipo (activos e inactivos), para la pantalla de gestión. */
    public List<ValorTipoPropiedadDto> listarTodos(Long tipoId) {
        return valorRepo.findByTipoIdOrderByOrdenAscValorAsc(tipoId).stream()
                .map(this::toDto).toList();
    }

    public ValorTipoPropiedadDto crear(Long tipoId, ValorTipoPropiedadDto req) {
        if (!tipoRepo.existsById(tipoId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de propiedad no encontrado");
        }
        String valor = normalizar(req.valor());
        if (valor.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El valor no puede estar vacío");
        }
        ValorTipoPropiedad v = new ValorTipoPropiedad();
        v.setTipoId(tipoId);
        v.setValor(valor);
        v.setParentValorId(req.parentValorId());
        v.setOrden(req.orden());
        v.setActivo(true);
        return toDto(guardarUnico(v));
    }

    public ValorTipoPropiedadDto actualizar(Long id, ValorTipoPropiedadDto req) {
        ValorTipoPropiedad v = valorRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Valor no encontrado"));
        String valor = normalizar(req.valor());
        if (valor.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El valor no puede estar vacío");
        }
        v.setValor(valor);
        v.setOrden(req.orden());
        v.setActivo(req.activo());
        return toDto(guardarUnico(v));
    }

    public void desactivar(Long id) {
        ValorTipoPropiedad v = valorRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Valor no encontrado"));
        v.setActivo(false);
        valorRepo.save(v);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /** Normaliza igual que Propiedad.identificador (trim + mayúsculas) para que coincidan. */
    public static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toUpperCase();
    }

    private ValorTipoPropiedad guardarUnico(ValorTipoPropiedad v) {
        try {
            return valorRepo.save(v);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe el valor '" + v.getValor() + "' para este tipo en esa rama");
        }
    }

    private ValorTipoPropiedadDto toDto(ValorTipoPropiedad v) {
        return new ValorTipoPropiedadDto(
                v.getId(), v.getTipoId(), v.getValor(),
                v.getParentValorId(), v.getOrden(), v.isActivo());
    }
}
