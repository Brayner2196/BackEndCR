package com.backendcr.residentialcomplex.config;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Utilidad central para operaciones de fecha/hora consciente del tenant activo.
 *
 * <p>Lee la timezone del {@link TenantContext} (seteada por {@code TenantFilter}).
 * Si no hay tenant en contexto (jobs schedulados, endpoints sin tenant), usa
 * "America/Bogota" como fallback.</p>
 *
 * <p>Todos los servicios de negocio deben usar estos métodos en lugar de
 * {@code LocalDate.now()} / {@code LocalDateTime.now()} para garantizar que
 * los cálculos de vencimiento sean correctos sin importar la zona del servidor.</p>
 */
public final class ColombiaTimeZone {

    /** Fallback cuando no hay tenant activo en el ThreadLocal. */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Bogota");

    private ColombiaTimeZone() {}

    /** ZoneId del tenant activo, con fallback a America/Bogota. */
    public static ZoneId zoneActual() {
        try {
            return ZoneId.of(TenantContext.getTimezone());
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }

    /** Fecha de hoy en la zona del tenant activo. Reemplaza {@code LocalDate.now()}. */
    public static LocalDate hoy() {
        return LocalDate.now(zoneActual());
    }

    /** Fecha y hora actual en la zona del tenant activo. Reemplaza {@code LocalDateTime.now()}. */
    public static LocalDateTime ahora() {
        return LocalDateTime.now(zoneActual());
    }

    /** ZonedDateTime para conversiones desde UTC. */
    public static ZonedDateTime ahoraZoned() {
        return ZonedDateTime.now(zoneActual());
    }
}
