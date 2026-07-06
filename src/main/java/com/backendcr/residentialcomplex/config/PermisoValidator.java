package com.backendcr.residentialcomplex.config;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;
import com.backendcr.residentialcomplex.entity.enums.CargoConsejo;
import com.backendcr.residentialcomplex.entity.enums.PermisoInquilino;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.InquilinoPermisoRepository;
import com.backendcr.residentialcomplex.repository.MiembroConsejoRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Componente usable desde SpEL en @PreAuthorize.
 *
 * Ejemplo de uso:
 *   @PreAuthorize("hasAnyRole('PROPIETARIO','INQUILINO') and
 *                  (hasRole('PROPIETARIO') or @permisoValidator.tienePermiso(#email,'VOTAR'))")
 */
@Component("permisoValidator")
@RequiredArgsConstructor
public class PermisoValidator {

    private final IdentidadRepository identidadRepo;
    private final UsuarioRepository usuarioRepo;
    private final InquilinoPermisoRepository permisoRepo;
    private final MiembroConsejoRepository miembroConsejoRepo;

    /**
     * Verifica si el usuario autenticado (por email) tiene el permiso indicado.
     * Los propietarios siempre retornan true (tienen todos los permisos implícitos).
     *
     * @param email   email del principal autenticado
     * @param permiso nombre del permiso (e.g. "VOTAR", "PQRS")
     */
    public boolean tienePermiso(String email, String permiso) {
        String tenantId = TenantContext.getTenant();
        if (tenantId == null) return false;

        return identidadRepo.findByEmailAndTenantId(email, tenantId).map(identidad -> {
            // Propietario siempre tiene acceso
            if ("PROPIETARIO".equals(identidad.getRol())) return true;
            if (!"INQUILINO".equals(identidad.getRol())) return false;

            return usuarioRepo.findByIdentidadId(identidad.getId()).map(usuario ->
                    permisoRepo.existsByInquilinoIdAndPermiso(
                            usuario.getId(),
                            PermisoInquilino.valueOf(permiso))
            ).orElse(false);
        }).orElse(false);
    }

    /**
     * Verifica si el usuario autenticado tiene membresía activa en el consejo comunal.
     * Útil como fallback en controladores que no requieren claim JWT.
     *
     * @param email email del principal autenticado
     */
    public boolean esConsejeroActivo(String email) {
        String tenantId = TenantContext.getTenant();
        if (tenantId == null) return false;

        return identidadRepo.findByEmailAndTenantId(email, tenantId).map(identidad ->
                usuarioRepo.findByIdentidadId(identidad.getId()).map(usuario ->
                        miembroConsejoRepo.existsByUsuarioIdAndActivoTrue(usuario.getId())
                ).orElse(false)
        ).orElse(false);
    }

    /**
     * Verifica si el usuario autenticado es el PRESIDENTE activo del consejo comunal.
     * Validación autoritativa contra BD (no confía solo en el claim del JWT, que
     * puede quedar desactualizado si el cargo cambia durante la vigencia del token).
     *
     * Uso en @PreAuthorize:
     *   @PreAuthorize("@permisoValidator.esPresidenteActivo(authentication.name)")
     *
     * @param email email del principal autenticado
     */
    public boolean esPresidenteActivo(String email) {
        return tieneCargoActivo(email, CargoConsejo.PRESIDENTE);
    }

    /**
     * Verifica si el usuario autenticado tiene un cargo específico activo en el consejo.
     * Reutilizable para futuras funciones restringidas por cargo (SECRETARIO, TESORERO...).
     */
    public boolean tieneCargoActivo(String email, CargoConsejo cargo) {
        String tenantId = TenantContext.getTenant();
        if (tenantId == null) return false;

        return identidadRepo.findByEmailAndTenantId(email, tenantId).map(identidad ->
                usuarioRepo.findByIdentidadId(identidad.getId()).map(usuario ->
                        miembroConsejoRepo.findByUsuarioIdAndActivoTrue(usuario.getId())
                                .map(m -> m.getCargo() == cargo)
                                .orElse(false)
                ).orElse(false)
        ).orElse(false);
    }
}
