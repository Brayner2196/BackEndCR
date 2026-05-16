package com.backendcr.residentialcomplex.dto.publicacion;

import com.backendcr.residentialcomplex.entity.Publicacion;
import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PublicacionResponse(
        Long id,
        Long vendedorId,
        String vendedorNombre,
        Long propiedadId,
        String titulo,
        String descripcion,
        BigDecimal precio,
        CategoriaPublicacion categoria,
        String contacto,
        EstadoPublicacion estado,
        LocalDateTime creadoEn,
        LocalDateTime actualizadoEn,
        /** Solo presente en resultados de marketplace — distancia de árbol al comprador. */
        Integer distanciaProximidad
) {
    /** Para uso sin proximidad (mis publicaciones, admin). */
    public static PublicacionResponse from(Publicacion p) {
        return new PublicacionResponse(
                p.getId(), p.getVendedorId(), p.getVendedorNombre(), p.getPropiedadId(),
                p.getTitulo(), p.getDescripcion(), p.getPrecio(), p.getCategoria(),
                p.getContacto(), p.getEstado(), p.getCreadoEn(), p.getActualizadoEn(), null);
    }

    /** Para uso con proximidad calculada. */
    public static PublicacionResponse from(Publicacion p, int distancia) {
        return new PublicacionResponse(
                p.getId(), p.getVendedorId(), p.getVendedorNombre(), p.getPropiedadId(),
                p.getTitulo(), p.getDescripcion(), p.getPrecio(), p.getCategoria(),
                p.getContacto(), p.getEstado(), p.getCreadoEn(), p.getActualizadoEn(), distancia);
    }
}
