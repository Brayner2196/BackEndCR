package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.parqueadero.*;
import com.backendcr.residentialcomplex.entity.ConfiguracionParqueadero;
import com.backendcr.residentialcomplex.entity.Parqueadero;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Vehiculo;
import com.backendcr.residentialcomplex.entity.enums.ModeloParqueaderoPrivado;
import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;
import com.backendcr.residentialcomplex.repository.ConfiguracionParqueaderoRepository;
import com.backendcr.residentialcomplex.repository.ParqueaderoRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParqueaderoService {

    private final ParqueaderoRepository parqueaderoRepo;
    private final VehiculoRepository vehiculoRepo;
    private final PropiedadRepository propiedadRepo;
    private final PropiedadService propiedadService;
    private final ConfiguracionParqueaderoRepository configRepo;

    // ─── Consultas ─────────────────────────────────────────────
    // Solo existen registros PRIVADOS. Los comunales son solo un conteo
    // en configuracion_parqueadero, no tienen registros individuales.

    public List<ParqueaderoResponse> listarTodos() {
        return parqueaderoRepo.findAll().stream().map(this::toResponse).toList();
    }

    public List<ParqueaderoResponse> listarPorPropiedad(Long propiedadId) {
        return parqueaderoRepo.findAllByPropiedadId(propiedadId).stream().map(this::toResponse).toList();
    }

    // ─── Creación bulk ─────────────────────────────────────────

    @Transactional
    public ParqueaderoBulkResultado crearBulk(ParqueaderoBulkRequest req) {
        // Leer el modelo configurado para este tenant (default ACCESORIO si no hay config)
        ModeloParqueaderoPrivado modelo = configRepo.findFirstBy()
                .map(ConfiguracionParqueadero::getModeloPrivadoDefault)
                .orElse(ModeloParqueaderoPrivado.ACCESORIO);

        List<ParqueaderoBulkResultado.ItemResultado> resultados = new ArrayList<>();
        int creados = 0;
        int duplicados = 0;

        for (ParqueaderoBulkRequest.Item item : req.items()) {
            if (parqueaderoRepo.existsByIdentificador(item.identificador())) {
                resultados.add(new ParqueaderoBulkResultado.ItemResultado(
                        item.identificador(), "DUPLICADO", null));
                duplicados++;
            } else {
                Parqueadero p = new Parqueadero();
                p.setIdentificador(item.identificador());
                p.setTipo(TipoParqueadero.PRIVADO);
                p.setModeloPropiedad(modelo);
                Parqueadero saved = parqueaderoRepo.save(p);
                resultados.add(new ParqueaderoBulkResultado.ItemResultado(
                        item.identificador(), "CREADO", saved.getId()));
                creados++;
            }
        }

        return new ParqueaderoBulkResultado(creados, duplicados, resultados);
    }

    // ─── Asignación de propiedad (admin) ──────────────────────

    @Transactional
    public ParqueaderoResponse asignarPropiedad(Long id, Long propiedadId) {
        Parqueadero p = parqueaderoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Parqueadero no encontrado"));

        // Si se reasigna a otra propiedad, liberar el vehículo anterior
        if (p.getVehiculoId() != null
                && (propiedadId == null || !propiedadId.equals(p.getPropiedadId()))) {
            vehiculoRepo.findById(p.getVehiculoId())
                    .ifPresent(v -> { v.setParqueaderoId(null); vehiculoRepo.save(v); });
            p.setVehiculoId(null);
        }

        p.setPropiedadId(propiedadId);
        return toResponse(parqueaderoRepo.save(p));
    }

    // ─── Asignación / desasignación de vehículo (propietario) ────

    @Transactional
    public ParqueaderoResponse asignarVehiculo(Long parqueaderoId, Long vehiculoId, Long propiedadId) {
        Parqueadero parqueadero = parqueaderoRepo.findById(parqueaderoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Parqueadero no encontrado"));

        // Validar pertenencia según el modelo del parqueadero:
        //   ACCESORIO     → propiedadId del spot coincide con el del residente
        //   INDEPENDIENTE → propiedadParqueaderoId coincide (el residente es dueño de la propiedad-parqueadero)
        //   null (legacy) → se trata como ACCESORIO
        boolean esIndependiente =
                parqueadero.getModeloPropiedad() == ModeloParqueaderoPrivado.INDEPENDIENTE;

        boolean perteneceAlResidente = esIndependiente
                ? propiedadId.equals(parqueadero.getPropiedadParqueaderoId())
                : propiedadId.equals(parqueadero.getPropiedadId());

        if (!perteneceAlResidente) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Este parqueadero no pertenece a tu propiedad");
        }

        // ── Liberar vehículo anterior si existe ───────────────────
        if (parqueadero.getVehiculoId() != null) {
            vehiculoRepo.findById(parqueadero.getVehiculoId())
                    .ifPresent(v -> { v.setParqueaderoId(null); vehiculoRepo.save(v); });
        }

        // ── Desasignar: vehiculoId null → limpiar y salir ─────────
        if (vehiculoId == null) {
            parqueadero.setVehiculoId(null);
            return toResponse(parqueaderoRepo.save(parqueadero));
        }

        // ── Asignar: validar que el vehículo sea del residente ────
        Vehiculo vehiculo = vehiculoRepo.findById(vehiculoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        // El vehículo siempre está ligado al apartamento/propiedad del residente.
        // Para INDEPENDIENTE el residente puede tener vehículos en su apto (propiedadId)
        // y en su propiedad-parqueadero; aceptamos ambos casos.
        boolean vehiculoEsDelResidente = propiedadId.equals(vehiculo.getPropiedadId())
                || (esIndependiente && propiedadId.equals(parqueadero.getPropiedadParqueaderoId())
                        && vehiculo.getPropiedadId() != null);

        if (!vehiculoEsDelResidente) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El vehículo no pertenece a tu propiedad");
        }

        parqueadero.setVehiculoId(vehiculoId);
        vehiculo.setParqueaderoId(parqueaderoId);
        vehiculoRepo.save(vehiculo);

        return toResponse(parqueaderoRepo.save(parqueadero));
    }

    // ─── Borrar (solo admin, solo si vacío) ───────────────────

    @Transactional
    public void eliminar(Long id) {
        Parqueadero p = parqueaderoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Parqueadero no encontrado"));

        if (p.getVehiculoId() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El parqueadero tiene un vehículo asignado. Retíralo antes de borrar.");
        }

        parqueaderoRepo.delete(p);
    }

    // ─── Helper toResponse ─────────────────────────────────────

    private ParqueaderoResponse toResponse(Parqueadero p) {
        // El identificador de la propiedad puede provenir de:
        //   ACCESORIO     → propiedadId (apartamento dueño)
        //   INDEPENDIENTE → propiedadParqueaderoId (propiedad-parqueadero en el árbol)
        Long propiedadRefId = p.getPropiedadId() != null
                ? p.getPropiedadId()
                : p.getPropiedadParqueaderoId();
        String propiedadIdentificador = null;
        if (propiedadRefId != null) {
            propiedadIdentificador = propiedadRepo.findById(propiedadRefId)
                    .map(Propiedad::getIdentificador).orElse(null);
        }
        // Path corto de la propiedad asociada (para mostrar a qué propiedad pertenece)
        String propiedadPath = propiedadService.pathCortoDePropiedad(propiedadRefId);

        String vehiculoPlaca = null;
        String vehiculoTipo = null;
        if (p.getVehiculoId() != null) {
            Vehiculo v = vehiculoRepo.findById(p.getVehiculoId()).orElse(null);
            if (v != null) {
                vehiculoPlaca = v.getPlaca();
                vehiculoTipo = v.getTipo().name();
            }
        }

        return new ParqueaderoResponse(
                p.getId(),
                p.getIdentificador(),
                p.getTipo(),
                p.getModeloPropiedad(),
                p.getPropiedadId(),
                propiedadIdentificador,
                propiedadPath,
                p.getPropiedadParqueaderoId(),
                p.getVehiculoId(),
                vehiculoPlaca,
                vehiculoTipo
        );
    }
}
