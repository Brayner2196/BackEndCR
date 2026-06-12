package com.backendcr.residentialcomplex.service.cartera;

import com.backendcr.residentialcomplex.dto.cartera.*;
import com.backendcr.residentialcomplex.entity.CondicionRegla;
import com.backendcr.residentialcomplex.entity.EstadoCartera;
import com.backendcr.residentialcomplex.entity.ReglaEstadoCartera;
import com.backendcr.residentialcomplex.entity.RestriccionEstado;
import com.backendcr.residentialcomplex.entity.enums.*;
import com.backendcr.residentialcomplex.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * CRUD de la configuración de cartera (estados + reglas + restricciones) del
 * tenant. Usa el patrón de replace anidado de ZonaComunService: al guardar un
 * estado, se borran y recrean sus reglas/condiciones/restricciones.
 */
@Service
@RequiredArgsConstructor
public class EstadoCarteraConfigService {

    private final EstadoCarteraRepository estadoRepo;
    private final ReglaEstadoCarteraRepository reglaRepo;
    private final CondicionReglaRepository condicionRepo;
    private final RestriccionEstadoRepository restriccionRepo;

    // ─── Lectura ───────────────────────────────────────────────────────────

    public List<EstadoCarteraConfigResponse> listar() {
        // Incluye inactivos para que el admin pueda reactivarlos desde la config.
        return estadoRepo.findAll().stream()
                .sorted(Comparator.comparingInt(EstadoCartera::getSeveridad).reversed())
                .map(this::toResponse)
                .toList();
    }

    // ─── Crear / Actualizar ────────────────────────────────────────────────

    @Transactional
    public EstadoCarteraConfigResponse crear(EstadoCarteraConfigRequest req) {
        estadoRepo.findByCodigo(req.codigo()).ifPresent(e -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un estado con el código " + req.codigo());
        });
        EstadoCartera estado = aplicarCampos(new EstadoCartera(), req);
        estado = estadoRepo.save(estado);
        sincronizarReglas(estado.getId(), req.reglas());
        sincronizarRestricciones(estado.getId(), req.restricciones());
        return toResponse(estado);
    }

    @Transactional
    public EstadoCarteraConfigResponse actualizar(Long id, EstadoCarteraConfigRequest req) {
        EstadoCartera estado = obtener(id);
        estadoRepo.findByCodigo(req.codigo())
                .filter(e -> !e.getId().equals(id))
                .ifPresent(e -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe otro estado con el código " + req.codigo());
                });
        aplicarCampos(estado, req);
        estado = estadoRepo.save(estado);
        sincronizarReglas(estado.getId(), req.reglas());
        sincronizarRestricciones(estado.getId(), req.restricciones());
        return toResponse(estado);
    }

    @Transactional
    public void eliminar(Long id) {
        EstadoCartera estado = obtener(id);
        borrarReglasYCondiciones(id);
        restriccionRepo.deleteByEstadoCarteraId(id);
        estadoRepo.delete(estado);
    }

    // ─── Seed de estados por defecto ───────────────────────────────────────

    @Transactional
    public List<EstadoCarteraConfigResponse> sembrarDefaults() {
        if (estadoRepo.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existen estados configurados; el sembrado solo aplica cuando está vacío");
        }

        crear(new EstadoCarteraConfigRequest("AL_DIA", "Al día", "Sin obligaciones vencidas",
                0, "#3F7A4F", true, true, List.of(), List.of()));

        crear(new EstadoCarteraConfigRequest("VENCIDA", "Vencida", "Tiene al menos un cobro vencido",
                10, "#9A6B00", false, true,
                List.of(new ReglaDto(null, "Algún cobro vencido", OperadorLogico.AND, 0,
                        List.of(new CondicionDto(CampoCartera.DIAS_VENCIDO_MAX, OperadorComparacion.MAYOR_IGUAL, BigDecimal.ONE)))),
                List.of()));

        crear(new EstadoCarteraConfigRequest("MORA", "En mora", "Mora relevante en días y monto",
                20, "#B45000", false, true,
                List.of(new ReglaDto(null, "+30 días y +$100.000", OperadorLogico.AND, 0,
                        List.of(
                                new CondicionDto(CampoCartera.DIAS_VENCIDO_MAX, OperadorComparacion.MAYOR_IGUAL, BigDecimal.valueOf(30)),
                                new CondicionDto(CampoCartera.MONTO_ADEUDADO, OperadorComparacion.MAYOR_IGUAL, BigDecimal.valueOf(100000))
                        ))),
                List.of(
                        new RestriccionDto(AccionRestringible.RESERVAR_ZONA_COMUN, "No puedes reservar mientras estés en mora"),
                        new RestriccionDto(AccionRestringible.ACCESO_VEHICULAR, "Acceso vehicular restringido por mora")
                )));

        crear(new EstadoCarteraConfigRequest("COBRO_PREJURIDICO", "Cobro prejurídico", "Mora avanzada",
                30, "#A34A4A", false, true,
                List.of(new ReglaDto(null, "+90 días o +3 periodos", OperadorLogico.OR, 0,
                        List.of(
                                new CondicionDto(CampoCartera.DIAS_VENCIDO_MAX, OperadorComparacion.MAYOR_IGUAL, BigDecimal.valueOf(90)),
                                new CondicionDto(CampoCartera.NUM_PERIODOS_VENCIDOS, OperadorComparacion.MAYOR_IGUAL, BigDecimal.valueOf(3))
                        ))),
                List.of(
                        new RestriccionDto(AccionRestringible.RESERVAR_ZONA_COMUN, "Cuenta en cobro prejurídico"),
                        new RestriccionDto(AccionRestringible.ACCESO_VEHICULAR, "Acceso vehicular restringido"),
                        new RestriccionDto(AccionRestringible.VOTAR_ASAMBLEA, "Sin derecho a voto por cartera vencida")
                )));

        return listar();
    }

    /** Catálogo de acciones disponibles para la UI. */
    public AccionRestringible[] accionesDisponibles() {
        return AccionRestringible.values();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────

    private EstadoCartera aplicarCampos(EstadoCartera e, EstadoCarteraConfigRequest req) {
        e.setCodigo(req.codigo());
        e.setNombre(req.nombre());
        e.setDescripcion(req.descripcion());
        e.setSeveridad(req.severidad() != null ? req.severidad() : 0);
        e.setColor(req.color());
        if (req.esPositivo() != null) e.setEsPositivo(req.esPositivo());
        if (req.activo() != null) e.setActivo(req.activo());
        return e;
    }

    private void sincronizarReglas(Long estadoId, List<ReglaDto> reglasDto) {
        borrarReglasYCondiciones(estadoId);
        if (reglasDto == null) return;
        int orden = 0;
        for (ReglaDto dto : reglasDto) {
            ReglaEstadoCartera regla = new ReglaEstadoCartera();
            regla.setEstadoCarteraId(estadoId);
            regla.setNombre(dto.nombre() != null ? dto.nombre() : "Regla");
            regla.setOperadorLogico(dto.operadorLogico() != null ? dto.operadorLogico() : OperadorLogico.AND);
            regla.setOrden(dto.orden() != null ? dto.orden() : orden++);
            regla.setActiva(true);
            regla = reglaRepo.save(regla);

            if (dto.condiciones() != null) {
                for (CondicionDto cDto : dto.condiciones()) {
                    CondicionRegla c = new CondicionRegla();
                    c.setReglaId(regla.getId());
                    c.setCampo(cDto.campo());
                    c.setOperador(cDto.operador());
                    c.setValor(cDto.valor());
                    condicionRepo.save(c);
                }
            }
        }
    }

    private void borrarReglasYCondiciones(Long estadoId) {
        for (ReglaEstadoCartera r : reglaRepo.findByEstadoCarteraIdOrderByOrdenAsc(estadoId)) {
            condicionRepo.deleteByReglaId(r.getId());
        }
        reglaRepo.deleteByEstadoCarteraId(estadoId);
    }

    private void sincronizarRestricciones(Long estadoId, List<RestriccionDto> restriccionesDto) {
        restriccionRepo.deleteByEstadoCarteraId(estadoId);
        if (restriccionesDto == null) return;
        for (RestriccionDto dto : restriccionesDto) {
            if (dto.accion() == null) continue;
            RestriccionEstado r = new RestriccionEstado();
            r.setEstadoCarteraId(estadoId);
            r.setAccion(dto.accion());
            r.setMensaje(dto.mensaje());
            restriccionRepo.save(r);
        }
    }

    private EstadoCartera obtener(Long id) {
        return estadoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Estado de cartera no encontrado"));
    }

    private EstadoCarteraConfigResponse toResponse(EstadoCartera e) {
        List<ReglaDto> reglas = new ArrayList<>();
        for (ReglaEstadoCartera r : reglaRepo.findByEstadoCarteraIdOrderByOrdenAsc(e.getId())) {
            List<CondicionDto> condiciones = condicionRepo.findByReglaId(r.getId()).stream()
                    .map(c -> new CondicionDto(c.getCampo(), c.getOperador(), c.getValor()))
                    .toList();
            reglas.add(new ReglaDto(r.getId(), r.getNombre(), r.getOperadorLogico(), r.getOrden(), condiciones));
        }
        List<RestriccionDto> restricciones = restriccionRepo.findByEstadoCarteraId(e.getId()).stream()
                .map(x -> new RestriccionDto(x.getAccion(), x.getMensaje()))
                .toList();
        return new EstadoCarteraConfigResponse(
                e.getId(), e.getCodigo(), e.getNombre(), e.getDescripcion(),
                e.getSeveridad(), e.getColor(), e.isEsPositivo(), e.isActivo(),
                reglas, restricciones);
    }
}
