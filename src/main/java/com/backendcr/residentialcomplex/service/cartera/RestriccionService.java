package com.backendcr.residentialcomplex.service.cartera;

import com.backendcr.residentialcomplex.dto.cartera.ResultadoRestriccion;
import com.backendcr.residentialcomplex.entity.EstadoCartera;
import com.backendcr.residentialcomplex.entity.EstadoCarteraPropiedad;
import com.backendcr.residentialcomplex.entity.RestriccionEstado;
import com.backendcr.residentialcomplex.entity.enums.AccionRestringible;
import com.backendcr.residentialcomplex.repository.EstadoCarteraPropiedadRepository;
import com.backendcr.residentialcomplex.repository.EstadoCarteraRepository;
import com.backendcr.residentialcomplex.repository.RestriccionEstadoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Punto ÚNICO de aplicación de restricciones por estado de cartera. Cualquier
 * feature (reservas, vigilante, paz y salvo…) consulta aquí en vez de decidir
 * por su cuenta. Degradación segura: sin snapshot o sin configuración → permitido.
 */
@Service
@RequiredArgsConstructor
public class RestriccionService {

    private final EstadoCarteraPropiedadRepository snapshotRepo;
    private final EstadoCarteraRepository estadoRepo;
    private final RestriccionEstadoRepository restriccionRepo;

    /** Forma booleana simple. */
    public boolean puede(Long propiedadId, AccionRestringible accion) {
        return verificar(propiedadId, accion).permitido();
    }

    /** Forma detallada: incluye estado y mensaje configurado para mostrar al usuario. */
    public ResultadoRestriccion verificar(Long propiedadId, AccionRestringible accion) {
        Optional<EstadoCarteraPropiedad> snapOpt = snapshotRepo.findByPropiedadId(propiedadId);
        if (snapOpt.isEmpty()) return ResultadoRestriccion.permitir();

        EstadoCartera estado = estadoRepo.findById(snapOpt.get().getEstadoCarteraId()).orElse(null);
        if (estado == null) return ResultadoRestriccion.permitir();

        Optional<RestriccionEstado> restriccion =
                restriccionRepo.findByEstadoCarteraIdAndAccion(estado.getId(), accion);
        if (restriccion.isEmpty()) return ResultadoRestriccion.permitir();

        String mensaje = restriccion.get().getMensaje() != null
                ? restriccion.get().getMensaje()
                : "Acción no permitida: la unidad está en estado " + estado.getNombre();
        return ResultadoRestriccion.bloqueado(estado.getCodigo(), estado.getNombre(), mensaje);
    }
}
