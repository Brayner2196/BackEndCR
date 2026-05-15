package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.pago.ConfiguracionMoraRequest;
import com.backendcr.residentialcomplex.dto.pago.ConfiguracionMoraResponse;
import com.backendcr.residentialcomplex.entity.ConfiguracionMora;
import com.backendcr.residentialcomplex.entity.enums.TipoCalculoMora;
import com.backendcr.residentialcomplex.repository.ConfiguracionMoraRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ConfiguracionMoraService {

    private final ConfiguracionMoraRepository moraRepo;

    /** Configuración de mora activa actual. Puede ser vacío si nunca se ha configurado. */
    public Optional<ConfiguracionMoraResponse> obtenerActiva() {
        return moraRepo.findFirstByActivoTrueOrderByFechaVigenciaDesc()
                .map(ConfiguracionMoraResponse::from);
    }

    /** Histórico completo de configuraciones de mora, del más reciente al más antiguo. */
    public List<ConfiguracionMoraResponse> listarHistorico() {
        return moraRepo.findAll().stream()
                .sorted((a, b) -> b.getFechaVigencia().compareTo(a.getFechaVigencia()))
                .map(ConfiguracionMoraResponse::from)
                .toList();
    }

    /**
     * Crea una nueva configuración de mora y desactiva la anterior.
     * Valida que los campos requeridos según el tipo de cálculo estén presentes.
     */
    @Transactional
    public ConfiguracionMoraResponse crear(ConfiguracionMoraRequest req) {
        // Validar campos según tipo de cálculo
        if (req.tipoCalculo() == TipoCalculoMora.PORCENTAJE) {
            if (req.porcentajeMensual() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El porcentaje mensual es requerido para el tipo PORCENTAJE");
            }
        } else if (req.tipoCalculo() == TipoCalculoMora.MONTO_FIJO) {
            if (req.montoFijo() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El monto fijo es requerido para el tipo MONTO_FIJO");
            }
        }

        // Desactivar configuración anterior
        moraRepo.findFirstByActivoTrueOrderByFechaVigenciaDesc()
                .ifPresent(anterior -> {
                    anterior.setActivo(false);
                    moraRepo.save(anterior);
                });

        // Crear nueva configuración
        ConfiguracionMora nueva = new ConfiguracionMora();
        nueva.setTipoCalculo(req.tipoCalculo());
        nueva.setPorcentajeMensual(req.porcentajeMensual());
        nueva.setMontoFijo(req.montoFijo());
        nueva.setDiasGracia(req.diasGracia());
        nueva.setFechaVigencia(req.fechaVigencia());
        nueva.setActivo(true);

        return ConfiguracionMoraResponse.from(moraRepo.save(nueva));
    }
}
