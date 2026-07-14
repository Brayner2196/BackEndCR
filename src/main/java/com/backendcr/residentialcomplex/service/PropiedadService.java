package com.backendcr.residentialcomplex.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.backendcr.residentialcomplex.dto.propiedad.PropiedadPathItemDto;
import com.backendcr.residentialcomplex.dto.propiedad.PropiedadRequest;
import com.backendcr.residentialcomplex.dto.propiedad.PropiedadResponse;
import com.backendcr.residentialcomplex.dto.propiedad.ResidenteResumenDto;
import com.backendcr.residentialcomplex.dto.propiedad.TipoPropiedadNodoDto;
import com.backendcr.residentialcomplex.dto.propiedad.UsuarioPropiedadResponse;
import com.backendcr.residentialcomplex.dto.vigilancia.PropiedadOpcionPage;
import com.backendcr.residentialcomplex.dto.vigilancia.PropiedadOpcionResponse;
import com.backendcr.residentialcomplex.entity.Parqueadero;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.TipoPropiedad;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.UsuarioPropiedad;
import com.backendcr.residentialcomplex.entity.enums.EstadoPropiedad;
import com.backendcr.residentialcomplex.entity.enums.ModeloParqueaderoPrivado;
import com.backendcr.residentialcomplex.entity.enums.TipoParqueadero;
import com.backendcr.residentialcomplex.repository.IdentidadRepository;
import com.backendcr.residentialcomplex.repository.ParqueaderoRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.TipoPropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PropiedadService {

    private final TipoPropiedadRepository tipoRepo;
    private final PropiedadRepository propiedadRepo;
    private final ParqueaderoRepository parqueaderoRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;
    private final UsuarioRepository usuarioRepo;
    private final IdentidadRepository identidadRepo;
    private final PropiedadPathCalculator pathCalculator;
    private final ValorPropiedadValidator valorValidator;

    // ── Tipos de propiedad ────────────────────────────────────────────────────

    public List<TipoPropiedadNodoDto> obtenerArbol() {
        return obtenerArbol(false);
    }

    public List<TipoPropiedadNodoDto> obtenerArbol(boolean soloFacturables) {
        return tipoRepo.findByParentIdIsNullOrderByOrden().stream()
                .filter(TipoPropiedad::isActivo)
                .filter(t -> !soloFacturables || t.isEsFacturable())
                .map(this::toNodoDto)
                .toList();
    }

    public TipoPropiedadNodoDto crearTipo(TipoPropiedadNodoDto request) {
        TipoPropiedad tipo = new TipoPropiedad();
        tipo.setNombre(request.nombre());
        tipo.setDescripcion(request.descripcion());
        tipo.setParentId(request.parentId());
        tipo.setOrden(request.orden());
        tipo.setActivo(true);
        tipo.setEsFacturable(request.esFacturable());
        tipo.setEsParqueadero(request.esParqueadero());
        return toNodoDto(tipoRepo.save(tipo));
    }

    public TipoPropiedadNodoDto actualizarTipo(Long id, TipoPropiedadNodoDto request) {
        TipoPropiedad tipo = tipoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo no encontrado"));
        tipo.setNombre(request.nombre());
        tipo.setDescripcion(request.descripcion());
        tipo.setOrden(request.orden());
        tipo.setEsFacturable(request.esFacturable());
        tipo.setEsParqueadero(request.esParqueadero());
        return toNodoDto(tipoRepo.save(tipo));
    }

    public void desactivarTipo(Long id) {
        TipoPropiedad tipo = tipoRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo no encontrado"));
        tipo.setActivo(false);
        tipoRepo.save(tipo);
    }

    // ── Propiedades ───────────────────────────────────────────────────────────

    public List<PropiedadResponse> listarTodas() {
        return propiedadRepo.findAll().stream()
                .map(p -> toPropiedadResponse(p, true))
                .toList();
    }

    /**
     * Selector paginado de propiedades FACTURABLES con buscador, para portería
     * (registro de paquetes/visitas). El path corto, el filtro, el orden y la
     * paginación los resuelve una CTE recursiva en la BD (sin N+1 ni carga total
     * en memoria). Compatible con el multitenancy por schema (search_path).
     */
    public PropiedadOpcionPage buscarFacturablesParaSelector(String buscar, int page, int size) {
        int p = Math.max(page, 0);
        int s = size <= 0 ? 20 : Math.min(size, 100);
        String q = buscar == null ? "" : buscar.trim();

        // Columnas: [0]=id, [1]=identificador, [2]=path_corto, [3]=total
        List<Object[]> filas = propiedadRepo.buscarFacturablesSelector(q, s, p * s);

        long total = filas.isEmpty() ? 0 : ((Number) filas.get(0)[3]).longValue();
        int totalPages = (int) Math.ceil((double) total / s);
        boolean last = p >= totalPages - 1;

        List<PropiedadOpcionResponse> content = filas.stream()
                .map(f -> new PropiedadOpcionResponse(
                        ((Number) f[0]).longValue(),
                        (String) f[1],
                        (String) f[2]))
                .toList();

        return new PropiedadOpcionPage(content, p, s, total, totalPages, last);
    }

    @Transactional
    public PropiedadResponse crear(PropiedadRequest request) {
        Long propiedadHojaId = resolverOCrearPath(request.propiedadPath());
        Propiedad hoja = propiedadRepo.findById(propiedadHojaId).orElseThrow();

        // Si el tipo de propiedad hoja es un parqueadero, auto-crear el spot físico
        tipoRepo.findById(hoja.getTipoId()).ifPresent(tipo -> {
            if (tipo.isEsParqueadero()
                    && !parqueaderoRepo.existsByPropiedadParqueaderoId(hoja.getId())) {
                Parqueadero spot = new Parqueadero();
                spot.setIdentificador(hoja.getIdentificador());
                spot.setTipo(TipoParqueadero.PRIVADO);
                spot.setModeloPropiedad(ModeloParqueaderoPrivado.INDEPENDIENTE);
                spot.setPropiedadParqueaderoId(hoja.getId());
                parqueaderoRepo.save(spot);
            }
        });

        return toPropiedadResponse(hoja, false);
    }

    @Transactional
    public PropiedadResponse actualizarEstado(Long id, EstadoPropiedad nuevoEstado) {
        Propiedad propiedad = propiedadRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Propiedad no encontrada"));
        propiedad.setEstado(nuevoEstado);
        return toPropiedadResponse(propiedadRepo.save(propiedad), false);
    }

    // ── Asignaciones usuario-propiedad ────────────────────────────────────────

    @Transactional
    public void asignarUsuario(Long propiedadId, Long usuarioId) {
        if (!propiedadRepo.existsById(propiedadId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Propiedad no encontrada");
        }
        if (!usuarioRepo.existsById(usuarioId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado");
        }
        if (usuarioPropiedadRepo.existsByUsuarioIdAndPropiedadId(usuarioId, propiedadId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El usuario ya está asignado a esta propiedad");
        }

        boolean esPrimera = usuarioPropiedadRepo.findByUsuarioId(usuarioId).isEmpty();

        UsuarioPropiedad up = new UsuarioPropiedad();
        up.setUsuarioId(usuarioId);
        up.setPropiedadId(propiedadId);
        up.setEsPrincipal(esPrimera);
        usuarioPropiedadRepo.save(up);
    }

    @Transactional
    public void quitarUsuario(Long propiedadId, Long usuarioId) {
        UsuarioPropiedad up = usuarioPropiedadRepo
                .findByUsuarioIdAndPropiedadId(usuarioId, propiedadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        usuarioPropiedadRepo.delete(up);

        if (up.isEsPrincipal()) {
            usuarioPropiedadRepo.findByUsuarioId(usuarioId).stream()
                    .findFirst()
                    .ifPresent(siguiente -> {
                        siguiente.setEsPrincipal(true);
                        usuarioPropiedadRepo.save(siguiente);
                    });
        }
    }

    @Transactional
    public void marcarComoPrincipal(Long propiedadId, Long usuarioId) {
        UsuarioPropiedad up = usuarioPropiedadRepo
                .findByUsuarioIdAndPropiedadId(usuarioId, propiedadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada"));
        usuarioPropiedadRepo.findByUsuarioId(usuarioId).forEach(u -> {
            u.setEsPrincipal(false);
            usuarioPropiedadRepo.save(u);
        });
        up.setEsPrincipal(true);
        usuarioPropiedadRepo.save(up);
    }

    public List<UsuarioPropiedadResponse> getMisPropiedades(Long usuarioId) {
        return usuarioPropiedadRepo.findByUsuarioId(usuarioId).stream()
                .map(up -> {
                    Propiedad p = propiedadRepo.findById(up.getPropiedadId()).orElse(null);
                    if (p == null) return null;
                    String path = construirPathTexto(p);
                    String tipoRaiz = obtenerNombreTipoRaiz(p);
                    String pathCorto = pathCalculator.pathCortoDe(p);
                    boolean esParqueadero = obtenerEsParqueaderoRaiz(p);
                    return new UsuarioPropiedadResponse(up.getId(), p.getId(), path, pathCorto, tipoRaiz, p.getEstado(), up.isEsPrincipal(), esParqueadero);
                })
                .filter(r -> r != null)
                .toList();
    }

    public Long resolverOCrearPath(List<PropiedadPathItemDto> path) {
        if (path == null || path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El path de propiedad no puede estar vacío");
        }

        // Barrera de seguridad: rechaza valores fuera del catálogo permitido
        // antes de crear cualquier nodo (evita "ABCDERGRTGG" y similares).
        valorValidator.validarPath(path);

        Long parentPropiedadId = null;
        Propiedad actual = null;
        // Prefijo acumulado del path corto (top-down): evita recalcular hacia arriba.
        String prefijoPath = "";

        for (PropiedadPathItemDto item : path) {
            String valorNormalizado = item.valor().trim().toUpperCase();
            Long finalParent = parentPropiedadId;
            String pathCortoNodo = pathCalculator.concat(prefijoPath, valorNormalizado);
            Optional<Propiedad> existente = propiedadRepo
                    .findByTipoIdAndIdentificadorAndParentId(item.tipoId(), valorNormalizado, finalParent);

            if (existente.isPresent()) {
                actual = existente.get();
                // Backfill defensivo: si un nodo previo no tenia path, lo completa.
                if (actual.getPathCorto() == null || actual.getPathCorto().isBlank()) {
                    actual.setPathCorto(pathCortoNodo);
                    actual = propiedadRepo.save(actual);
                }
            } else {
                actual = new Propiedad();
                actual.setTipoId(item.tipoId());
                actual.setIdentificador(valorNormalizado);
                actual.setParentId(parentPropiedadId);
                actual.setEstado(EstadoPropiedad.DISPONIBLE);
                actual.setPathCorto(pathCortoNodo);
                actual = propiedadRepo.save(actual);
            }
            parentPropiedadId = actual.getId();
            prefijoPath = actual.getPathCorto();
        }

        return actual != null?actual.getId():null;
    }

    /**
     * Igual que resolverOCrearPath pero sin crear nada: solo busca.
     * Lanza 404 si algún nivel del path no existe.
     * Usar cuando el path debe referenciar una propiedad ya existente (ej: asignar un inquilino).
     */
    public Long resolverPathSoloExistente(List<PropiedadPathItemDto> path) {
        if (path == null || path.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El path de propiedad no puede estar vacío");
        }

        Long parentPropiedadId = null;
        Propiedad actual = null;

        for (PropiedadPathItemDto item : path) {
            String valorNormalizado = item.valor().trim().toUpperCase();
            Long finalParent = parentPropiedadId;
            actual = propiedadRepo
                    .findByTipoIdAndIdentificadorAndParentId(item.tipoId(), valorNormalizado, finalParent)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "No existe la propiedad '" + item.valor().trim() + "'. Verifique el path ingresado."));
            parentPropiedadId = actual.getId();
        }

        return actual != null ? actual.getId() : null;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TipoPropiedadNodoDto toNodoDto(TipoPropiedad tipo) {
        List<TipoPropiedad> hijosEntidad = tipoRepo.findByParentIdOrderByOrden(tipo.getId());
        List<TipoPropiedadNodoDto> hijosDto = hijosEntidad.stream()
                .filter(TipoPropiedad::isActivo)
                .map(this::toNodoDto)
                .toList();
        return new TipoPropiedadNodoDto(
                tipo.getId(), tipo.getNombre(), tipo.getDescripcion(),
                tipo.getParentId(), tipo.getOrden(), tipo.isActivo(),
                tipo.isEsFacturable(), tipo.isEsParqueadero(), hijosDto);
    }

    private PropiedadResponse toPropiedadResponse(Propiedad p, boolean incluirResidentes) {
        TipoPropiedad tipoHoja = tipoRepo.findById(p.getTipoId()).orElse(null);
        String nombreTipo     = tipoHoja != null ? tipoHoja.getNombre() : "?";
        boolean esFacturable  = tipoHoja != null && tipoHoja.isEsFacturable();
        boolean esParqueadero = tipoHoja != null && tipoHoja.isEsParqueadero();
        String path      = construirPathTexto(p);
        String pathCorto = pathCalculator.pathCortoDe(p);

        List<ResidenteResumenDto> residentes = Collections.emptyList();
        if (incluirResidentes) {
            residentes = usuarioPropiedadRepo.findByPropiedadId(p.getId()).stream()
                    .map(up -> {
                        Usuario u = usuarioRepo.findById(up.getUsuarioId()).orElse(null);
                        if (u == null) return null;
                        String email = identidadRepo.findById(u.getIdentidadId())
                                .map(i -> i.getEmail()).orElse("");
                        return new ResidenteResumenDto(u.getId(), u.getNombre(), email, up.isEsPrincipal());
                    })
                    .filter(r -> r != null)
                    .toList();
        }

        return new PropiedadResponse(p.getId(), p.getTipoId(), nombreTipo,
                p.getParentId(), p.getIdentificador(), path, pathCorto,
                esFacturable, esParqueadero, p.getEstado(), residentes);
    }

    /** Path corto de una propiedad por id (reutilizable). Null si no existe. */
    public String pathCortoDePropiedad(Long propiedadId) {
        if (propiedadId == null) return null;
        return propiedadRepo.findById(propiedadId)
                .map(pathCalculator::pathCortoDe).orElse(null);
    }

    private String construirPathTexto(Propiedad hoja) {
        List<String> partes = new ArrayList<>();
        Propiedad actual = hoja;
        while (actual != null) {
            String nombreTipo = tipoRepo.findById(actual.getTipoId())
                    .map(TipoPropiedad::getNombre).orElse("?");
            partes.add(0, nombreTipo + " " + actual.getIdentificador());
            if (actual.getParentId() == null) break;
            actual = propiedadRepo.findById(actual.getParentId()).orElse(null);
        }
        return String.join(" / ", partes);
    }

    private String obtenerNombreTipoRaiz(Propiedad hoja) {
        Propiedad actual = hoja;
        while (actual.getParentId() != null) {
            actual = propiedadRepo.findById(actual.getParentId()).orElse(actual);
        }
        return tipoRepo.findById(actual.getTipoId()).map(TipoPropiedad::getNombre).orElse("?");
    }

    private boolean obtenerEsParqueaderoRaiz(Propiedad hoja) {
        Propiedad actual = hoja;
        while (actual.getParentId() != null) {
            actual = propiedadRepo.findById(actual.getParentId()).orElse(actual);
        }
        return tipoRepo.findById(actual.getTipoId()).map(TipoPropiedad::isEsParqueadero).orElse(false);
    }
}
