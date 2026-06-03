package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.parqueadero.ConfiguracionParqueaderoRequest;
import com.backendcr.residentialcomplex.dto.parqueadero.ConfiguracionParqueaderoResponse;
import com.backendcr.residentialcomplex.entity.ConfiguracionParqueadero;
import com.backendcr.residentialcomplex.repository.ConfiguracionParqueaderoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ConfiguracionParqueaderoService {

    private final ConfiguracionParqueaderoRepository repo;

    public ConfiguracionParqueaderoResponse obtener() {
        return repo.findFirstBy()
                .map(this::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Configuración de parqueadero no encontrada"));
    }

    @Transactional
    public ConfiguracionParqueaderoResponse guardar(ConfiguracionParqueaderoRequest req) {
        validarTotales(req);
        ConfiguracionParqueadero config = repo.findFirstBy().orElse(new ConfiguracionParqueadero());
        config.setTotalParqueaderos(req.totalParqueaderos());
        config.setParqueaderosComunes(req.parqueaderosComunes());
        config.setParqueaderosPrivados(req.parqueaderosPrivados());
        config.setMaxVehiculosPorPropiedad(req.maxVehiculosPorPropiedad());
        config.setPermiteCarro(req.permiteCarro());
        config.setPermiteMoto(req.permiteMoto());
        config.setPermiteBicicleta(req.permiteBicicleta());
        config.setRequiereAprobacionVehiculo(req.requiereAprobacionVehiculo());
        if (req.modeloPrivadoDefault() != null) {
            config.setModeloPrivadoDefault(req.modeloPrivadoDefault());
        }
        config.setAceptaParqueaderoVisitantes(req.aceptaParqueaderoVisitantes());
        config.setTotalParqueaderosVisitantes(req.totalParqueaderosVisitantes());
        return toResponse(repo.save(config));
    }

    // ── Helpers ───────────────────────────────────────────────

    private void validarTotales(ConfiguracionParqueaderoRequest req) {
        int visitantes = req.aceptaParqueaderoVisitantes() ? req.totalParqueaderosVisitantes() : 0;
        if (req.parqueaderosComunes() + req.parqueaderosPrivados() + visitantes > req.totalParqueaderos()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La suma de comunes, privados y visitantes no puede superar el total");
        }
    }

    public ConfiguracionParqueadero obtenerEntidad() {
        return repo.findFirstBy()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Configuración de parqueadero no encontrada"));
    }

    private ConfiguracionParqueaderoResponse toResponse(ConfiguracionParqueadero c) {
        return new ConfiguracionParqueaderoResponse(
                c.getId(),
                c.getTotalParqueaderos(),
                c.getParqueaderosComunes(),
                c.getParqueaderosPrivados(),
                c.getMaxVehiculosPorPropiedad(),
                c.isPermiteCarro(),
                c.isPermiteMoto(),
                c.isPermiteBicicleta(),
                c.isRequiereAprobacionVehiculo(),
                c.getModeloPrivadoDefault(),
                c.isAceptaParqueaderoVisitantes(),
                c.getTotalParqueaderosVisitantes()
        );
    }
}
