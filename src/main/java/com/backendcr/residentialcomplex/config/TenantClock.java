package com.backendcr.residentialcomplex.config;

import com.backendcr.residentialcomplex.config.multitenant.TenantContext;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/**
 * Reloj consciente del tenant activo.
 *
 * <p>Regla de arquitectura:</p>
 * <ul>
 *   <li><b>Instantes</b> (cuando paso algo: verificaciones, decisiones, auditoria):
 *       se generan y persisten SIEMPRE en UTC con {@link #ahora()} ({@link Instant}).
 *       La conversion a la zona del conjunto la hace el cliente (Flutter).</li>
 *   <li><b>Fechas civiles</b> (dia de calendario: vencimientos, periodos, "es hoy"):
 *       se resuelven en la zona del tenant con {@link #hoy()} ({@link LocalDate}),
 *       porque "que dia es hoy" depende de donde esta el conjunto. No se convierten
 *       en el cliente.</li>
 * </ul>
 *
 * <p>La zona se lee del {@link TenantContext} (seteada por {@code TenantFilter}).
 * Si no hay tenant en contexto (jobs, endpoints sin tenant), cae a "America/Bogota".</p>
 */
public final class TenantClock {

    /** Fallback cuando no hay tenant activo en el ThreadLocal. */
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Bogota");

    private TenantClock() {}

    /** ZoneId del tenant activo, con fallback a {@link #DEFAULT_ZONE}. */
    public static ZoneId zona() {
        try {
            return ZoneId.of(TenantContext.getTimezone());
        } catch (Exception e) {
            return DEFAULT_ZONE;
        }
    }

    /**
     * Instante actual en UTC. Usar para CUALQUIER campo que represente "cuando paso algo"
     * y que se persista. Reemplaza a {@code LocalDateTime.now()} en esos casos.
     */
    public static Instant ahora() {
        return Instant.now();
    }

    /**
     * Fecha civil de hoy en la zona del tenant. Usar para vencimientos, periodos y
     * comparaciones de calendario. Reemplaza a {@code LocalDate.now()}.
     */
    public static LocalDate hoy() {
        return LocalDate.now(zona());
    }

    /**
     * Fecha y hora de pared en la zona del tenant. SOLO para calculos contra valores
     * civiles del conjunto (p. ej. anticipacion de una reserva cuya hora es local).
     * NO usar para persistir instantes; para eso usar {@link #ahora()}.
     */
    public static LocalDateTime ahoraEnZona() {
        return LocalDateTime.now(zona());
    }

    /**
     * Convierte un texto ISO-8601 a {@link Instant} UTC para persistir.
     * <ul>
     *   <li>Si trae zona/offset ("...Z" o "...-05:00") se respeta tal cual.</li>
     *   <li>Si es local sin zona ("yyyy-MM-ddTHH:mm:ss") se interpreta como hora
     *       de pared del conjunto y se convierte a UTC con la zona del tenant.</li>
     * </ul>
     * Usar para inputs de fecha-hora del cliente (programacion de anuncios, votaciones).
     */
    public static Instant aInstante(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return OffsetDateTime.parse(iso).toInstant();
        } catch (DateTimeParseException e) {
            return LocalDateTime.parse(iso).atZone(zona()).toInstant();
        }
    }
}
