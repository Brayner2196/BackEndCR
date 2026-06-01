package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.vehiculo.DecisionVehiculoRequest;
import com.backendcr.residentialcomplex.dto.vehiculo.VehiculoRequest;
import com.backendcr.residentialcomplex.dto.vehiculo.VehiculoResponse;
import com.backendcr.residentialcomplex.entity.ConfiguracionParqueadero;
import com.backendcr.residentialcomplex.entity.Parqueadero;
import com.backendcr.residentialcomplex.entity.Vehiculo;
import com.backendcr.residentialcomplex.entity.enums.EstadoVehiculo;
import com.backendcr.residentialcomplex.repository.ParqueaderoRepository;
import com.backendcr.residentialcomplex.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VehiculoService {

    private final VehiculoRepository vehiculoRepo;
    private final ParqueaderoRepository parqueaderoRepo;
    private final ConfiguracionParqueaderoService configService;

    // ─── Residente: registrar vehículo ─────────────────────────

    @Transactional
    public VehiculoResponse registrar(VehiculoRequest req, Long propiedadId) {
        var config = configService.obtenerEntidad();

        // Validar tipo permitido
        validarTipoPermitido(req, config);

        // Validar límite de vehículos
        long actuales = vehiculoRepo.countByPropiedadIdAndEstadoNot(propiedadId, EstadoVehiculo.RECHAZADO);
        if (actuales >= config.getMaxVehiculosPorPropiedad()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Se alcanzó el límite de " + config.getMaxVehiculosPorPropiedad() + " vehículos por propiedad");
        }

        // Validar placa duplicada en la propiedad
        if (vehiculoRepo.existsByPlacaAndPropiedadId(req.placa(), propiedadId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un vehículo con esa placa en esta propiedad");
        }

        Vehiculo v = new Vehiculo();
        v.setPlaca(req.placa().toUpperCase());
        v.setTipo(req.tipo());
        v.setMarca(req.marca());
        v.setModelo(req.modelo());
        v.setColor(req.color());
        v.setPropiedadId(propiedadId);
        // Si no requiere aprobación, se aprueba automáticamente
        v.setEstado(config.isRequiereAprobacionVehiculo()
                ? EstadoVehiculo.PENDIENTE
                : EstadoVehiculo.APROBADO);

        return toResponse(vehiculoRepo.save(v));
    }

    // ─── Residente: listar vehículos de su propiedad ───────────

    public List<VehiculoResponse> listarPorPropiedad(Long propiedadId) {
        return vehiculoRepo.findAllByPropiedadId(propiedadId).stream()
                .map(this::toResponse).toList();
    }

    // ─── Residente: eliminar vehículo propio ───────────────────

    @Transactional
    public void eliminar(Long vehiculoId, Long propiedadId) {
        Vehiculo v = vehiculoRepo.findById(vehiculoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));

        if (!propiedadId.equals(v.getPropiedadId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso sobre este vehículo");
        }

        // Liberar parqueadero si estaba asignado
        if (v.getParqueaderoId() != null) {
            parqueaderoRepo.findById(v.getParqueaderoId())
                    .ifPresent(p -> { p.setVehiculoId(null); parqueaderoRepo.save(p); });
        }

        vehiculoRepo.delete(v);
    }

    // ─── Admin: listar pendientes ──────────────────────────────

    public List<VehiculoResponse> listarPendientes() {
        return vehiculoRepo.findAllByEstado(EstadoVehiculo.PENDIENTE).stream()
                .map(this::toResponse).toList();
    }

    public List<VehiculoResponse> listarTodos() {
        return vehiculoRepo.findAll().stream().map(this::toResponse).toList();
    }

    // ─── Admin: aprobar / rechazar ─────────────────────────────

    @Transactional
    public VehiculoResponse aprobar(Long vehiculoId) {
        Vehiculo v = obtenerPendiente(vehiculoId);
        v.setEstado(EstadoVehiculo.APROBADO);
        v.setMotivoRechazo(null);
        return toResponse(vehiculoRepo.save(v));
    }

    @Transactional
    public VehiculoResponse rechazar(Long vehiculoId, DecisionVehiculoRequest req) {
        Vehiculo v = obtenerPendiente(vehiculoId);
        v.setEstado(EstadoVehiculo.RECHAZADO);
        v.setMotivoRechazo(req != null ? req.motivo() : null);
        return toResponse(vehiculoRepo.save(v));
    }

    // ─── Helpers ───────────────────────────────────────────────

    private Vehiculo obtenerPendiente(Long id) {
        Vehiculo v = vehiculoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehículo no encontrado"));
        if (v.getEstado() != EstadoVehiculo.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El vehículo no está en estado PENDIENTE");
        }
        return v;
    }

    private void validarTipoPermitido(VehiculoRequest req, ConfiguracionParqueadero config) {
        boolean permitido = switch (req.tipo()) {
            case CARRO -> config.isPermiteCarro();
            case MOTO -> config.isPermiteMoto();
            case BICICLETA -> config.isPermiteBicicleta();
        };
        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El tipo de vehículo " + req.tipo() + " no está permitido en este conjunto");
        }
    }

    private VehiculoResponse toResponse(Vehiculo v) {
        String parqueaderoIdentificador = null;
        if (v.getParqueaderoId() != null) {
            parqueaderoIdentificador = parqueaderoRepo.findById(v.getParqueaderoId())
                    .map(Parqueadero::getIdentificador).orElse(null);
        }
        return new VehiculoResponse(
                v.getId(),
                v.getPlaca(),
                v.getTipo(),
                v.getMarca(),
                v.getModelo(),
                v.getColor(),
                v.getPropiedadId(),
                v.getParqueaderoId(),
                parqueaderoIdentificador,
                v.getEstado(),
                v.getMotivoRechazo()
        );
    }
}
