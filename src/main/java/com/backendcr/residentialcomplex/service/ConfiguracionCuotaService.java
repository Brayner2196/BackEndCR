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

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionCuotaService {

    private final ConfiguracionCuotaRepository cuotaRepo;

    /** Todo el histórico, de más reciente a más antiguo. */
    public List<ConfiguracionCuotaResponse> listarHistorico() {
        return cuotaRepo.findAllByOrderByFechaVigenciaDesdeDesc()
                .stream().map(ConfiguracionCuotaResponse::from).toList();
    }

    /** Solo cuotas marcadas activo = true. */
    public List<ConfiguracionCuotaResponse> listar() {
        return cuotaRepo.findAllByActivoTrue()
                .stream().map(ConfiguracionCuotaResponse::from).toList();
    }

    @Transactional
    public ConfiguracionCuotaResponse crear(ConfiguracionCuotaRequest req) {
        LocalDate desde = req.fechaVigenciaDesde();
        // Para la validación usamos 9999-12-31 cuando no hay fecha fin (open-ended).
        LocalDate hasta = req.fechaVigenciaHasta() != null
                ? req.fechaVigenciaHasta()
                : LocalDate.of(9999, 12, 31);

        if (req.fechaVigenciaHasta() != null && req.fechaVigenciaHasta().isBefore(desde)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha de fin no puede ser anterior a la fecha de inicio");
        }

        final Long EXCLUDE_NONE = 0L; // ID inexistente → no excluye nada

        if (req.propiedadId() != null) {
            if (cuotaRepo.existeSolapamientoPorPropiedad(req.propiedadId(), desde, hasta, EXCLUDE_NONE)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Ya existe una cuota para esa propiedad que se solapa con el período indicado");
            }
        } else if (req.tipoPropiedadId() != null) {
            if (req.numeroDesde() != null) {
                int rangoHasta = req.numeroHasta() != null ? req.numeroHasta() : Integer.MAX_VALUE;
                if (cuotaRepo.existeSolapamientoRangoPorTipo(
                        req.tipoPropiedadId(), req.numeroDesde(), rangoHasta, desde, hasta, EXCLUDE_NONE)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe una cuota para ese tipo con rango numérico solapado en las fechas indicadas");
                }
            } else {
                if (cuotaRepo.existeSolapamientoGeneralPorTipo(req.tipoPropiedadId(), desde, hasta, EXCLUDE_NONE)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Ya existe una cuota general para ese tipo que se solapa con el período indicado");
                }
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe especificar tipoPropiedadId o propiedadId");
        }

        ConfiguracionCuota cuota = new ConfiguracionCuota();
        cuota.setTipoPropiedadId(req.tipoPropiedadId());
        cuota.setPropiedadId(req.propiedadId());
        cuota.setTipoPropiedadCondicionId(req.tipoPropiedadCondicionId());
        cuota.setNumeroDesde(req.numeroDesde());
        cuota.setNumeroHasta(req.numeroHasta());
        cuota.setMonto(req.monto());
        cuota.setPeriodicidad(req.periodicidad());
        cuota.setFechaVigenciaDesde(req.fechaVigenciaDesde());
        cuota.setFechaVigenciaHasta(req.fechaVigenciaHasta());
        cuota.setActivo(true);
        return ConfiguracionCuotaResponse.from(cuotaRepo.save(cuota));
    }

    @Transactional
    public void desactivar(Long id) {
        ConfiguracionCuota cuota = cuotaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Configuración no encontrada"));
        cuota.setActivo(false);
        cuotaRepo.save(cuota);
    }
}
