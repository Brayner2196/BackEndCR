package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.presupuesto.*;
import com.backendcr.residentialcomplex.entity.CategoriaPresupuesto;
import com.backendcr.residentialcomplex.entity.GastoRegistrado;
import com.backendcr.residentialcomplex.entity.Presupuesto;
import com.backendcr.residentialcomplex.repository.CategoriaPresupuestoRepository;
import com.backendcr.residentialcomplex.repository.GastoRegistradoRepository;
import com.backendcr.residentialcomplex.repository.PresupuestoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PresupuestoService {

    private final PresupuestoRepository presupuestoRepo;
    private final CategoriaPresupuestoRepository categoriaRepo;
    private final GastoRegistradoRepository gastoRepo;

    // ── Admin: Presupuestos ────────────────────────────────────────────────────

    /** Lista todos los presupuestos (solo resumen, sin categorías) */
    public List<PresupuestoResponse> listarTodos() {
        return presupuestoRepo.findAllByOrderByAnioDesc().stream()
                .map(p -> {
                    BigDecimal ejecutado = gastoRepo.sumarMontoPorPresupuesto(p.getId());
                    return PresupuestoResponse.fromSummary(p, ejecutado);
                })
                .toList();
    }

    /** Detalle completo: presupuesto + categorías + gastos por categoría */
    public PresupuestoResponse obtenerDetalle(Long id) {
        Presupuesto p = findPresupuesto(id);
        BigDecimal ejecutadoTotal = gastoRepo.sumarMontoPorPresupuesto(id);
        List<CategoriaPresupuestoResponse> categorias = buildCategorias(id, true);
        return PresupuestoResponse.fromDetail(p, ejecutadoTotal, categorias);
    }

    /** Retorna el presupuesto activo actual con categorías (sin gastos individuales) */
    public PresupuestoResponse obtenerActivo() {
        Presupuesto p = presupuestoRepo.findFirstByActivoTrueOrderByAnioDesc()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No hay presupuesto activo"));
        BigDecimal ejecutadoTotal = gastoRepo.sumarMontoPorPresupuesto(p.getId());
        List<CategoriaPresupuestoResponse> categorias = buildCategorias(p.getId(), false);
        return PresupuestoResponse.fromDetail(p, ejecutadoTotal, categorias);
    }

    /** Crea un presupuesto con sus categorías */
    @Transactional
    public PresupuestoResponse crear(PresupuestoRequest req) {
        if (presupuestoRepo.findByAnio(req.anio()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un presupuesto para el año " + req.anio());
        }

        Presupuesto p = new Presupuesto();
        p.setAnio(req.anio());
        p.setTitulo(req.titulo());
        p.setActivo(req.activo());

        if (req.activo()) desactivarActuales();

        BigDecimal total = BigDecimal.ZERO;
        for (CategoriaPresupuestoRequest c : req.categorias()) {
            total = total.add(c.montoAsignado());
        }
        p.setMontoTotalPresupuestado(total);
        presupuestoRepo.save(p);

        guardarCategorias(p.getId(), req.categorias());

        return PresupuestoResponse.fromDetail(p, BigDecimal.ZERO, buildCategorias(p.getId(), false));
    }

    /** Actualiza presupuesto (reemplaza categorías) */
    @Transactional
    public PresupuestoResponse actualizar(Long id, PresupuestoRequest req) {
        Presupuesto p = findPresupuesto(id);

        // Si hay otro presupuesto con ese año (no este), conflicto
        presupuestoRepo.findByAnio(req.anio()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe un presupuesto para el año " + req.anio());
            }
        });

        if (req.activo() && !p.isActivo()) desactivarActuales();

        p.setAnio(req.anio());
        p.setTitulo(req.titulo());
        p.setActivo(req.activo());

        // Recalcular total
        BigDecimal total = req.categorias().stream()
                .map(CategoriaPresupuestoRequest::montoAsignado)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        p.setMontoTotalPresupuestado(total);
        presupuestoRepo.save(p);

        // Reemplazar categorías: elimina gastos huérfanos y recrea
        List<CategoriaPresupuesto> viejas = categoriaRepo.findByPresupuestoIdOrderByNombreAsc(id);
        for (CategoriaPresupuesto vieja : viejas) {
            gastoRepo.deleteByCategoriaId(vieja.getId());
        }
        categoriaRepo.deleteByPresupuestoId(id);
        guardarCategorias(id, req.categorias());

        BigDecimal ejecutadoTotal = gastoRepo.sumarMontoPorPresupuesto(id);
        return PresupuestoResponse.fromDetail(p, ejecutadoTotal, buildCategorias(id, false));
    }

    /** Activa o desactiva un presupuesto */
    @Transactional
    public PresupuestoResponse toggleActivo(Long id, boolean activo) {
        Presupuesto p = findPresupuesto(id);
        if (activo) desactivarActuales();
        p.setActivo(activo);
        presupuestoRepo.save(p);
        BigDecimal ejecutado = gastoRepo.sumarMontoPorPresupuesto(id);
        return PresupuestoResponse.fromDetail(p, ejecutado, buildCategorias(id, false));
    }

    // ── Admin: Gastos ──────────────────────────────────────────────────────────

    /** Registra un nuevo gasto en una categoría del presupuesto */
    @Transactional
    public GastoRegistradoResponse registrarGasto(Long presupuestoId, GastoRegistradoRequest req, Long adminId) {
        findPresupuesto(presupuestoId);

        CategoriaPresupuesto cat = categoriaRepo.findById(req.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoría no encontrada"));

        if (!cat.getPresupuestoId().equals(presupuestoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La categoría no pertenece a este presupuesto");
        }

        GastoRegistrado g = new GastoRegistrado();
        g.setPresupuestoId(presupuestoId);
        g.setCategoriaId(req.categoriaId());
        g.setDescripcion(req.descripcion());
        g.setMonto(req.monto());
        g.setFecha(req.fecha());
        g.setComprobante(req.comprobante());
        g.setRegistradoPor(adminId);
        gastoRepo.save(g);

        return GastoRegistradoResponse.from(g);
    }

    /** Elimina un gasto registrado */
    @Transactional
    public void eliminarGasto(Long presupuestoId, Long gastoId) {
        GastoRegistrado g = gastoRepo.findById(gastoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Gasto no encontrado"));
        if (!g.getPresupuestoId().equals(presupuestoId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El gasto no pertenece a este presupuesto");
        }
        gastoRepo.delete(g);
    }

    // ── Helpers internos ──────────────────────────────────────────────────────

    private Presupuesto findPresupuesto(Long id) {
        return presupuestoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Presupuesto no encontrado"));
    }

    private void desactivarActuales() {
        presupuestoRepo.findFirstByActivoTrueOrderByAnioDesc().ifPresent(actual -> {
            actual.setActivo(false);
            presupuestoRepo.save(actual);
        });
    }

    private void guardarCategorias(Long presupuestoId, List<CategoriaPresupuestoRequest> lista) {
        for (CategoriaPresupuestoRequest req : lista) {
            CategoriaPresupuesto c = new CategoriaPresupuesto();
            c.setPresupuestoId(presupuestoId);
            c.setNombre(req.nombre());
            c.setDescripcion(req.descripcion());
            c.setMontoAsignado(req.montoAsignado());
            c.setColor(req.color());
            c.setIcono(req.icono());
            categoriaRepo.save(c);
        }
    }

    /**
     * Construye la lista de respuesta de categorías para un presupuesto.
     *
     * @param conGastos si true, incluye la lista de gastos individuales por categoría
     */
    private List<CategoriaPresupuestoResponse> buildCategorias(Long presupuestoId, boolean conGastos) {
        return categoriaRepo.findByPresupuestoIdOrderByNombreAsc(presupuestoId).stream()
                .map(c -> {
                    BigDecimal ejecutado = gastoRepo.sumarMontoPorCategoria(c.getId());
                    if (conGastos) {
                        List<GastoRegistradoResponse> gastos = gastoRepo
                                .findByCategoriaIdOrderByFechaDescCreadoEnDesc(c.getId())
                                .stream()
                                .map(GastoRegistradoResponse::from)
                                .toList();
                        return CategoriaPresupuestoResponse.from(c, ejecutado, gastos);
                    }
                    return CategoriaPresupuestoResponse.from(c, ejecutado);
                })
                .toList();
    }
}
