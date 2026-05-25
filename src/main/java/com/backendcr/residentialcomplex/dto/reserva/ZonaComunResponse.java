package com.backendcr.residentialcomplex.dto.reserva;

import com.backendcr.residentialcomplex.entity.HorarioGrupoZona;
import com.backendcr.residentialcomplex.entity.ZonaComun;
import com.backendcr.residentialcomplex.entity.enums.CategoriaZona;
import com.backendcr.residentialcomplex.entity.enums.ModoAprobacion;
import com.backendcr.residentialcomplex.entity.enums.ModoTarifa;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

public record ZonaComunResponse(
        Long id,
        String nombre,
        String descripcion,
        CategoriaZona categoria,
        Integer capacidad,
        boolean activa,

        // Modo de uso
        boolean usoExclusivo,
        int bufferLimpiezaMinutos,

        // Horario legacy
        LocalTime horaApertura,
        LocalTime horaCierre,
        String diasDisponibles,

        // Horarios por grupos
        List<HorarioGrupoDto> horarioGrupos,

        // Reglas de duración
        Integer duracionMinMinutos,
        Integer duracionMaxMinutos,

        // Reglas de anticipación
        Integer anticipacionMinDias,
        Integer anticipacionMaxDias,

        // Cuota por residente
        Integer maxReservasSemana,
        Integer maxReservasMes,
        Integer cancelacionHorasAntes,

        // Aprobación
        boolean requiereAprobacion,
        ModoAprobacion modoAprobacion,

        // Costo
        boolean tieneCosto,
        ModoTarifa modoTarifa,
        BigDecimal tarifaMonto,
        BigDecimal depositoMonto,

        // Restricciones
        boolean soloPropietarios,
        boolean sinDeudaPendiente,
        Integer edadMinima,
        String soloTorre,

        // Suspensión
        boolean suspendida,
        String motivoSuspension
) {
    public static ZonaComunResponse from(ZonaComun z, List<HorarioGrupoZona> grupos) {
        List<HorarioGrupoDto> gruposDto = grupos.stream()
                .map(g -> new HorarioGrupoDto(
                        g.getId(),
                        g.getEtiqueta(),
                        g.getDias(),
                        g.getNota(),
                        g.getOrden(),
                        g.getFranjas().stream()
                                .map(f -> new FranjaHorariaDto(
                                        f.getId(),
                                        fmtTime(f.getHoraInicio()),
                                        fmtTime(f.getHoraFin()),
                                        f.getOrden()))
                                .toList()
                ))
                .toList();

        return new ZonaComunResponse(
                z.getId(), z.getNombre(), z.getDescripcion(), z.getCategoria(),
                z.getCapacidad(), z.isActiva(),
                z.isUsoExclusivo(), z.getBufferLimpiezaMinutos(),
                z.getHoraApertura(), z.getHoraCierre(), z.getDiasDisponibles(),
                gruposDto,
                z.getDuracionMinMinutos(), z.getDuracionMaxMinutos(),
                z.getAnticipacionMinDias(), z.getAnticipacionMaxDias(),
                z.getMaxReservasSemana(), z.getMaxReservasMes(), z.getCancelacionHorasAntes(),
                z.isRequiereAprobacion(), z.getModoAprobacion(),
                z.isTieneCosto(), z.getModoTarifa(), z.getTarifaMonto(), z.getDepositoMonto(),
                z.isSoloPropietarios(), z.isSinDeudaPendiente(), z.getEdadMinima(), z.getSoloTorre(),
                z.isSuspendida(), z.getMotivoSuspension()
        );
    }

    private static String fmtTime(LocalTime t) {
        if (t == null) return null;
        return String.format("%02d:%02d", t.getHour(), t.getMinute());
    }
}
