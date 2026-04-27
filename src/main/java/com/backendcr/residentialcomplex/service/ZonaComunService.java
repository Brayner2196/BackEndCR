package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.reserva.ZonaComunRequest;
import com.backendcr.residentialcomplex.dto.reserva.ZonaComunResponse;
import com.backendcr.residentialcomplex.entity.ZonaComun;
import com.backendcr.residentialcomplex.repository.ZonaComunRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ZonaComunService {

    private final ZonaComunRepository zonaRepo;

    public List<ZonaComunResponse> listarTodas() {
        return zonaRepo.findAll().stream().map(ZonaComunResponse::from).toList();
    }

    public List<ZonaComunResponse> listarActivas() {
        return zonaRepo.findAllByActivaTrue().stream().map(ZonaComunResponse::from).toList();
    }

    @Transactional
    public ZonaComunResponse crear(ZonaComunRequest req) {
        ZonaComun z = new ZonaComun();
        z.setNombre(req.nombre());
        z.setDescripcion(req.descripcion());
        z.setCapacidad(req.capacidad() != null ? req.capacidad() : 0);
        z.setActiva(req.activa() == null || req.activa());
        return ZonaComunResponse.from(zonaRepo.save(z));
    }

    @Transactional
    public ZonaComunResponse actualizar(Long id, ZonaComunRequest req) {
        ZonaComun z = zonaRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Zona común no encontrada"));
        z.setNombre(req.nombre());
        z.setDescripcion(req.descripcion());
        if (req.capacidad() != null) z.setCapacidad(req.capacidad());
        if (req.activa() != null) z.setActiva(req.activa());
        return ZonaComunResponse.from(zonaRepo.save(z));
    }
}
