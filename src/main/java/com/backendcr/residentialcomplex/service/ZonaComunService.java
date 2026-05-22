package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.reserva.*;
import com.backendcr.residentialcomplex.entity.*;
import com.backendcr.residentialcomplex.entity.enums.TipoExcepcionZona;
import com.backendcr.residentialcomplex.repository.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaComunService {

    private final ZonaComunRepository zonaRepo;
    private final ExcepcionZonaComunRepository excepcionRepo;
    private final HorarioGrupoZonaRepository horarioGrupoRepo;

    // ─── Consultas ─────────────────────────────────────────────

    public List<ZonaComunResponse> listarTodas() {
        return zonaRepo.findAll().stream()
                .map(z -> ZonaComunResponse.from(z, horarioGrupoRepo.findByZonaComunIdOrderByOrdenAsc(z.getId())))
                .toList();
    }

    public List<ZonaComunResponse> listarActivas() {
        return zonaRepo.findAll().stream()
                .filter(z -> z.isActiva() && !z.isSuspendida())
                .map(z -> ZonaComunResponse.from(z, horarioGrupoRepo.findByZonaComunIdOrderByOrdenAsc(z.getId())))
                .toList();
    }

    // ─── Crear / Actualizar ────────────────────────────────────

    @Transactional
    public ZonaComunResponse crear(@Valid ZonaComunRequest req) {
        ZonaComun z = aplicarCampos(new ZonaComun(), req);
        z = zonaRepo.save(z);
        List<HorarioGrupoZona> grupos = sincronizarGrupos(z.getId(), req.horarioGrupos());
        return ZonaComunResponse.from(z, grupos);
    }

    @Transactional
    public ZonaComunResponse actualizar(Long id, @Valid ZonaComunRequest req) {
        ZonaComun z = obtener(id);
        aplicarCampos(z, req);
        z = zonaRepo.save(z);
        List<HorarioGrupoZona> grupos = sincronizarGrupos(z.getId(), req.horarioGrupos());
        return ZonaComunResponse.from(z, grupos);
    }

    private ZonaComun aplicarCampos(ZonaComun z, ZonaComunRequest req) {
        z.setNombre(req.nombre());
        z.setDescripcion(req.descripcion());
        if (req.categoria() != null)  z.setCategoria(req.categoria());
        if (req.capacidad() != null)  z.setCapacidad(req.capacidad());
        if (req.activa() != null)     z.setActiva(req.activa());

        if (req.usoExclusivo() != null)          z.setUsoExclusivo(req.usoExclusivo());
        if (req.bufferLimpiezaMinutos() != null)  z.setBufferLimpiezaMinutos(req.bufferLimpiezaMinutos());

        z.setHoraApertura(parseTime(req.horaApertura()));
        z.setHoraCierre(parseTime(req.horaCierre()));
        z.setDiasDisponibles(req.diasDisponibles());

        z.setDuracionMinMinutos(req.duracionMinMinutos());
        z.setDuracionMaxMinutos(req.duracionMaxMinutos());
        z.setAnticipacionMinDias(req.anticipacionMinDias());
        z.setAnticipacionMaxDias(req.anticipacionMaxDias());

        z.setMaxReservasSemana(req.maxReservasSemana());
        z.setMaxReservasMes(req.maxReservasMes());
        z.setCancelacionHorasAntes(req.cancelacionHorasAntes());

        if (req.modoAprobacion() != null) {
            z.setModoAprobacion(req.modoAprobacion());
        } else if (req.requiereAprobacion() != null) {
            z.setRequiereAprobacion(req.requiereAprobacion());
        }

        if (req.tieneCosto() != null) z.setTieneCosto(req.tieneCosto());
        if (req.modoTarifa() != null) z.setModoTarifa(req.modoTarifa());
        z.setTarifaMonto(req.tarifaMonto());
        z.setDepositoMonto(req.depositoMonto());

        if (req.soloPropietarios() != null)   z.setSoloPropietarios(req.soloPropietarios());
        if (req.sinDeudaPendiente() != null)  z.setSinDeudaPendiente(req.sinDeudaPendiente());
        z.setEdadMinima(req.edadMinima());
        z.setSoloTorre(req.soloTorre());

        return z;
    }

    private List<HorarioGrupoZona> sincronizarGrupos(Long zonaId, List<HorarioGrupoDto> gruposDto) {
        if (gruposDto == null) {
            return horarioGrupoRepo.findByZonaComunIdOrderByOrdenAsc(zonaId);
        }
        horarioGrupoRepo.deleteByZonaComunId(zonaId);

        List<HorarioGrupoZona> resultado = new ArrayList<>();
        int orden = 0;
        for (HorarioGrupoDto dto : gruposDto) {
            HorarioGrupoZona grupo = new HorarioGrupoZona();
            grupo.setZonaComunId(zonaId);
            grupo.setEtiqueta(dto.etiqueta());
            grupo.setDias(dto.dias());
            grupo.setNota(dto.nota());
            grupo.setOrden(orden++);

            if (dto.franjas() != null) {
                int fOrden = 0;
                for (FranjaHorariaDto fDto : dto.franjas()) {
                    FranjaHoraria f = new FranjaHoraria();
                    f.setHoraInicio(parseTime(fDto.horaInicio()));
                    f.setHoraFin(parseTime(fDto.horaFin()));
                    f.setOrden(fOrden++);
                    grupo.addFranja(f); // bidireccional: setea grupo en la franja
                }
            }
            resultado.add(horarioGrupoRepo.save(grupo));
        }
        return resultado;
    }

    // ─── Suspensión ────────────────────────────────────────────

    @Transactional
    public ZonaComunResponse suspender(Long id, String motivo) {
        ZonaComun z = obtener(id);
        if (z.isSuspendida()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La zona ya está suspendida");
        z.setSuspendida(true);
        z.setMotivoSuspension(motivo);
        z = zonaRepo.save(z);
        return ZonaComunResponse.from(z, horarioGrupoRepo.findByZonaComunIdOrderByOrdenAsc(id));
    }

    @Transactional
    public ZonaComunResponse reactivar(Long id) {
        ZonaComun z = obtener(id);
        if (!z.isSuspendida()) throw new ResponseStatusException(HttpStatus.CONFLICT, "La zona no está suspendida");
        z.setSuspendida(false);
        z.setMotivoSuspension(null);
        z = zonaRepo.save(z);
        return ZonaComunResponse.from(z, horarioGrupoRepo.findByZonaComunIdOrderByOrdenAsc(id));
    }

    // ─── Excepciones ───────────────────────────────────────────

    public List<ExcepcionZonaComunResponse> listarExcepciones(Long zonaId) {
        obtener(zonaId);
        return excepcionRepo.findAllByZonaComunIdOrderByFechaAsc(zonaId)
                .stream().map(ExcepcionZonaComunResponse::from).toList();
    }

    @Transactional
    public ExcepcionZonaComunResponse agregarExcepcion(Long zonaId, @Valid ExcepcionZonaComunRequest req) {
        obtener(zonaId);
        LocalTime apertura = parseTime(req.horaApertura());
        LocalTime cierre   = parseTime(req.horaCierre());
        LocalDate fecha    = parseDate(req.fecha());

        if (req.tipo() == TipoExcepcionZona.APERTURA_ESPECIAL) {
            if (apertura == null || cierre == null)
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APERTURA_ESPECIAL requiere horaApertura y horaCierre");
            if (!cierre.isAfter(apertura))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "horaCierre debe ser posterior a horaApertura");
        }

        excepcionRepo.findByZonaComunIdAndFecha(zonaId, fecha).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una excepción para la fecha " + fecha);
        });

        ExcepcionZonaComun ex = new ExcepcionZonaComun();
        ex.setZonaComunId(zonaId);
        ex.setFecha(fecha);
        ex.setTipo(req.tipo());
        ex.setHoraApertura(apertura);
        ex.setHoraCierre(cierre);
        ex.setMotivo(req.motivo());
        return ExcepcionZonaComunResponse.from(excepcionRepo.save(ex));
    }

    @Transactional
    public void eliminarExcepcion(Long zonaId, Long excepcionId) {
        obtener(zonaId);
        excepcionRepo.findById(excepcionId).ifPresent(e -> {
            if (!e.getZonaComunId().equals(zonaId))
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La excepción no pertenece a esta zona");
            excepcionRepo.delete(e);
        });
    }

    // ─── Helpers ───────────────────────────────────────────────

    public ZonaComun obtener(Long id) {
        return zonaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona común no encontrada"));
    }

    private LocalTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            String normalized = s.length() > 8 ? s.substring(0, 8) : s;
            if (normalized.length() == 5) normalized += ":00";
            return LocalTime.parse(normalized);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de hora inválido: " + s);
        }
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato de fecha inválido: " + s);
        }
    }
}
