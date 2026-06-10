package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.consejo.MiembroConsejoRequest;
import com.backendcr.residentialcomplex.dto.consejo.MiembroConsejoResponse;
import com.backendcr.residentialcomplex.entity.MiembroConsejo;
import com.backendcr.residentialcomplex.repository.MiembroConsejoRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsejoService {

    private final MiembroConsejoRepository miembroConsejoRepo;
    private final UsuarioRepository usuarioRepo;
    private final NotificacionService notificacionService;

    // ─── Admin ────────────────────────────────────────────────────────────────

    public List<MiembroConsejoResponse> listarActivos() {
        return miembroConsejoRepo.findAllByActivoTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MiembroConsejoResponse> listarHistorial() {
        return miembroConsejoRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MiembroConsejoResponse designar(MiembroConsejoRequest req) {
        // Validar que el usuario existe
        usuarioRepo.findById(req.usuarioId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuario no encontrado con id: " + req.usuarioId()));

        // Revocar membresía anterior si existe
        miembroConsejoRepo.findByUsuarioIdAndActivoTrue(req.usuarioId())
                .ifPresent(m -> {
                    m.setActivo(false);
                    miembroConsejoRepo.save(m);
                });

        MiembroConsejo nuevo = new MiembroConsejo();
        nuevo.setUsuarioId(req.usuarioId());
        nuevo.setCargo(req.cargo());
        nuevo.setFechaInicio(req.fechaInicio());
        nuevo.setFechaFin(req.fechaFin());
        nuevo.setActivo(true);
        nuevo = miembroConsejoRepo.save(nuevo);

        // Notificar al usuario designado
        notificacionService.enviarAUsuario(
                req.usuarioId(),
                "🏛️ Eres parte del Consejo Comunal",
                "Has sido designado como " + req.cargo().name().toLowerCase()
                        + " del consejo. Ya puedes acceder a las funciones del consejo en la app.",
                Map.of("tipo", "CONSEJO_DESIGNADO", "cargo", req.cargo().name())
        );

        return toResponse(nuevo);
    }

    @Transactional
    public MiembroConsejoResponse actualizar(Long id, MiembroConsejoRequest req) {
        MiembroConsejo miembro = miembroConsejoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Miembro de consejo no encontrado con id: " + id));

        miembro.setCargo(req.cargo());
        miembro.setFechaInicio(req.fechaInicio());
        miembro.setFechaFin(req.fechaFin());
        return toResponse(miembroConsejoRepo.save(miembro));
    }

    @Transactional
    public void revocar(Long id) {
        MiembroConsejo miembro = miembroConsejoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Miembro de consejo no encontrado con id: " + id));

        miembro.setActivo(false);
        miembroConsejoRepo.save(miembro);

        // Notificar al usuario revocado
        notificacionService.enviarAUsuario(
                miembro.getUsuarioId(),
                "Actualización de tu rol en el conjunto",
                "Tu participación en el consejo comunal ha finalizado.",
                Map.of("tipo", "CONSEJO_REVOCADO")
        );
    }

    // ─── Público (directorio del consejo — todos los residentes pueden verlo) ─

    public List<MiembroConsejoResponse> directorioPublico() {
        return listarActivos();
    }

    // ─── Mapper ───────────────────────────────────────────────────────────────

    private MiembroConsejoResponse toResponse(MiembroConsejo m) {
        String nombre = usuarioRepo.findById(m.getUsuarioId())
                .map(u -> u.getNombre())
                .orElse("Usuario desconocido");

        return new MiembroConsejoResponse(
                m.getId(),
                m.getUsuarioId(),
                nombre,
                m.getCargo(),
                m.getFechaInicio(),
                m.getFechaFin(),
                m.isActivo(),
                m.getCreadoEn()
        );
    }
}
