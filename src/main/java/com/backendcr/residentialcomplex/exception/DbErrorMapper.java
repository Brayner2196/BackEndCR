package com.backendcr.residentialcomplex.exception;

import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;

/**
 * Traduce errores de integridad de PostgreSQL a mensajes claros para el
 * cliente (Flutter), evitando exponer el stack trace crudo o el genérico
 * "Error interno del servidor".
 *
 * Lógica separada y reutilizable: cualquier handler puede llamar a
 * {@link #mensaje(Throwable)} y {@link #status(Throwable)}.
 *
 * Usa {@link java.sql.SQLException#getSQLState()} (estándar del JDK) para no
 * acoplar el driver de PostgreSQL (que está en scope runtime). El nombre de la
 * columna se extrae del texto del mensaje cuando está disponible.
 *
 * SQLState estándar de PostgreSQL:
 *   23502 not_null_violation     → falta un dato obligatorio
 *   23505 unique_violation       → registro duplicado
 *   23503 foreign_key_violation  → referencia inexistente
 *   23514 check_violation        → valor fuera de las reglas permitidas
 */
public final class DbErrorMapper {

    private DbErrorMapper() {}

    public static final String NOT_NULL    = "23502";
    public static final String UNIQUE      = "23505";
    public static final String FOREIGN_KEY = "23503";
    public static final String CHECK       = "23514";

    // Extrae el nombre entre comillas: ... column "schema_name" ...
    private static final Pattern COLUMNA = Pattern.compile("column \"([^\"]+)\"");

    /** Mensaje claro y en español listo para mostrarse en Flutter. */
    public static String mensaje(Throwable ex) {
        SQLException sql = extraerSql(ex);
        if (sql == null) {
            return "No se pudo completar la operación por una restricción de datos.";
        }

        String campo = etiquetaCampo(extraerColumna(sql.getMessage()));

        return switch (nullSafe(sql.getSQLState())) {
            case NOT_NULL    -> "Falta un dato obligatorio" + (campo != null ? ": " + campo : "") + ".";
            case UNIQUE      -> "Ya existe un registro con ese valor" + (campo != null ? " (" + campo + ")" : "") + ".";
            case FOREIGN_KEY -> "El registro relacionado no existe o está en uso.";
            case CHECK       -> "Un valor no cumple las reglas permitidas" + (campo != null ? ": " + campo : "") + ".";
            default          -> "No se pudo completar la operación por una restricción de datos.";
        };
    }

    /** Status HTTP apropiado según el tipo de violación. */
    public static HttpStatus status(Throwable ex) {
        SQLException sql = extraerSql(ex);
        String state = sql != null ? nullSafe(sql.getSQLState()) : "";
        return switch (state) {
            case UNIQUE, FOREIGN_KEY -> HttpStatus.CONFLICT;     // 409
            case NOT_NULL, CHECK     -> HttpStatus.BAD_REQUEST;  // 400
            default                  -> HttpStatus.BAD_REQUEST;
        };
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    /** Recorre la cadena de causas hasta encontrar una SQLException. */
    private static SQLException extraerSql(Throwable ex) {
        Throwable actual = ex;
        while (actual != null) {
            if (actual instanceof SQLException sql) {
                return sql;
            }
            actual = actual.getCause();
        }
        return null;
    }

    private static String extraerColumna(String mensaje) {
        if (mensaje == null) {
            return null;
        }
        Matcher m = COLUMNA.matcher(mensaje);
        return m.find() ? m.group(1) : null;
    }

    /** Convierte el nombre técnico de la columna en una etiqueta legible. */
    private static String etiquetaCampo(String columna) {
        if (columna == null || columna.isBlank()) {
            return null;
        }
        return switch (columna) {
            case "schemaname", "schema_name" -> "nombre del esquema";
            case "codigo"      -> "código";
            case "email"       -> "correo";
            case "nombre"      -> "nombre";
            default            -> columna.replace('_', ' ');
        };
    }

    private static String nullSafe(String s) {
        return s != null ? s : "";
    }
}
