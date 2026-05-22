package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.enums.CategoriaZona;
import com.backendcr.residentialcomplex.entity.enums.ModoAprobacion;
import com.backendcr.residentialcomplex.entity.enums.ModoTarifa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ZonaComunRequest(

        // ── Identidad ──────────────────────────────────────────────
        @NotBlank @Size(max = 100) String nombre,
        @Size(max = 500) String descripcion,
        CategoriaZona categoria,

        // ── Aforo y modo de uso ────────────────────────────────────
        @PositiveOrZero Integer capacidad,
        Boolean activa,
        Boolean usoExclusivo,
        Integer bufferLimpiezaMinutos,

        // ── Horario legacy (campo simple, compatibilidad) ──────────
        String horaApertura,
        String horaCierre,
        /** CSV de días: LUNES,MARTES,... Null = todos los días. */
        String diasDisponibles,

        // ── Horarios por grupos (modelo flexible) ──────────────────
        List<HorarioGrupoDto> horarioGrupos,

        // ── Reglas de duración (minutos) ───────────────────────────
        @PositiveOrZero Integer duracionMinMinutos,
        @PositiveOrZero Integer duracionMaxMinutos,

        // ── Reglas de anticipación (días) ──────────────────────────
        @PositiveOrZero Integer anticipacionMinDias,
        @PositiveOrZero Integer anticipacionMaxDias,

        // ── Cuota por residente ────────────────────────────────────
        Integer maxReservasSemana,
        Integer maxReservasMes,
        Integer cancelacionHorasAntes,

        // ── Aprobación ─────────────────────────────────────────────
        Boolean requiereAprobacion,
        ModoAprobacion modoAprobacion,

        // ── Costo ──────────────────────────────────────────────────
        Boolean tieneCosto,
        ModoTarifa modoTarifa,
        BigDecimal tarifaMonto,
        BigDecimal depositoMonto,

        // ── Restricciones de acceso ────────────────────────────────
        Boolean soloPropietarios,
        Boolean sinDeudaPendiente,
        Integer edadMinima,
        String soloTorre
) {}
