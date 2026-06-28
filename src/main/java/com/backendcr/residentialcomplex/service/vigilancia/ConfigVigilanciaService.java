package com.backendcr.residentialcomplex.service.vigilancia;

import com.backendcr.residentialcomplex.dto.vigilancia.ConfigVigilanciaDto;
import com.backendcr.residentialcomplex.entity.ConfigVigilancia;
import com.backendcr.residentialcomplex.repository.ConfigVigilanciaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestiona la parametrización del módulo de vigilancia (una fila por conjunto).
 */
@Service
@RequiredArgsConstructor
public class ConfigVigilanciaService {

    private final ConfigVigilanciaRepository repo;

    /** Devuelve la config del conjunto, creándola con valores por defecto si no existe. */
    @Transactional
    public ConfigVigilancia obtener() {
        return repo.findById(1L).orElseGet(() -> repo.save(new ConfigVigilancia()));
    }

    public ConfigVigilanciaDto obtenerDto() {
        return ConfigVigilanciaDto.from(obtener());
    }

    @Transactional
    public ConfigVigilanciaDto actualizar(ConfigVigilanciaDto dto) {
        ConfigVigilancia c = obtener();
        c.setExpiracionVisitaHoras(dto.expiracionVisitaHoras());
        c.setExigeDocumentoPeatonal(dto.exigeDocumentoPeatonal());
        c.setExigeFotoPaquete(dto.exigeFotoPaquete());
        c.setNotificarLlegadaPaquete(dto.notificarLlegadaPaquete());
        return ConfigVigilanciaDto.from(repo.save(c));
    }
}
