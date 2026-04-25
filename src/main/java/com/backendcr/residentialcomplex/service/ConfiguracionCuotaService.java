package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pago.ConfiguracionCuotaRequest;
import com.backendcr.residentialcomplex.dto.pago.ConfiguracionCuotaResponse;
import com.backendcr.residentialcomplex.entity.ConfiguracionCuota;
import com.backendcr.residentialcomplex.repository.ConfiguracionCuotaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionCuotaService {

    private final ConfiguracionCuotaRepository cuotaRepo;

    public List<ConfiguracionCuotaResponse> listar() {
        return cuotaRepo.findAllByActivoTrue().stream()
                .map(ConfiguracionCuotaResponse::from).toList();
    }

    @Transactional
    public ConfiguracionCuotaResponse crear(ConfiguracionCuotaRequest req) {
        if (req.propiedadId() != null) {
            cuotaRepo.findByPropiedadIdAndActivoTrue(req.propiedadId())
                    .ifPresent(c -> { c.setActivo(false); cuotaRepo.save(c); });
        } else if (req.tipoPropiedadId() != null) {
            cuotaRepo.findByTipoPropiedadIdAndActivoTrue(req.tipoPropiedadId())
                    .ifPresent(c -> { c.setActivo(false); cuotaRepo.save(c); });
        }
        ConfiguracionCuota cuota = new ConfiguracionCuota();
        cuota.setTipoPropiedadId(req.tipoPropiedadId());
        cuota.setPropiedadId(req.propiedadId());
        cuota.setMonto(req.monto());
        cuota.setPeriodicidad(req.periodicidad());
        cuota.setFechaVigenciaDesde(req.fechaVigenciaDesde());
        cuota.setActivo(true);
        return ConfiguracionCuotaResponse.from(cuotaRepo.save(cuota));
    }

    @Transactional
    public void desactivar(Long id) {
        ConfiguracionCuota cuota = cuotaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuración no encontrada"));
        cuota.setActivo(false);
        cuotaRepo.save(cuota);
    }
}
