package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.dto.publicacion.PublicacionRequest;
import com.backendcr.residentialcomplex.dto.publicacion.PublicacionResponse;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.Publicacion;
import com.backendcr.residentialcomplex.entity.Usuario;
import com.backendcr.residentialcomplex.entity.enums.CategoriaPublicacion;
import com.backendcr.residentialcomplex.entity.enums.EstadoPublicacion;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import com.backendcr.residentialcomplex.repository.PublicacionRepository;
import com.backendcr.residentialcomplex.repository.UsuarioPropiedadRepository;
import com.backendcr.residentialcomplex.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PublicacionService {

    private final PublicacionRepository publicacionRepo;
    private final UsuarioRepository usuarioRepo;
    private final PropiedadRepository propiedadRepo;
    private final UsuarioPropiedadRepository usuarioPropiedadRepo;

    // ─── Marketplace (listado público para residentes) ──────────

    /**
     * Devuelve publicaciones ACTIVAS, opcionalmente filtradas, ordenadas por
     * proximidad al árbol de propiedades del comprador.
     *
     * @param compradorId  ID del residente que busca
     * @param busqueda     texto libre (nullable)
     * @param categoria    filtro de categoría (nullable)
     */
    public List<PublicacionResponse> marketplace(Long compradorId, String busqueda,
                                                  CategoriaPublicacion categoria) {
        List<Publicacion> activas = publicacionRepo.buscarActivas(categoria,
                busqueda != null && busqueda.isBlank() ? null : busqueda);

        // Obtener propiedad principal del comprador para calcular proximidad
        Long propiedadCompradorId = propiedadPrincipalId(compradorId);

        // Construir cadena de ancestros del comprador una sola vez
        List<Long> cadenaComprador = construirCadena(propiedadCompradorId);

        // Mapear con distancia y ordenar
        return activas.stream()
                // Excluir las propias del comprador del marketplace
                .filter(p -> !p.getVendedorId().equals(compradorId))
                .map(p -> {
                    int distancia = calcularDistancia(cadenaComprador, construirCadena(p.getPropiedadId()));
                    return PublicacionResponse.from(p, distancia);
                })
                .sorted(Comparator
                        .comparingInt(PublicacionResponse::distanciaProximidad)
                        .thenComparing(Comparator.comparing(PublicacionResponse::creadoEn).reversed()))
                .toList();
    }

    // ─── Mis publicaciones ──────────────────────────────────────

    public List<PublicacionResponse> misPublicaciones(Long vendedorId) {
        return publicacionRepo.findByVendedorIdAndEstadoNotOrderByCreadoEnDesc(
                vendedorId, EstadoPublicacion.ELIMINADA)
                .stream().map(PublicacionResponse::from).toList();
    }

    // ─── CRUD ───────────────────────────────────────────────────

    @Transactional
    public PublicacionResponse crear(Long vendedorId, @Valid PublicacionRequest req) {
        Usuario vendedor = usuarioRepo.findById(vendedorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendedor no encontrado"));

        Publicacion p = new Publicacion();
        p.setVendedorId(vendedorId);
        p.setVendedorNombre(vendedor.getNombre());
        p.setPropiedadId(propiedadPrincipalId(vendedorId));
        aplicarCampos(p, req);
        return PublicacionResponse.from(publicacionRepo.save(p));
    }

    @Transactional
    public PublicacionResponse actualizar(Long publicacionId, Long vendedorId,
                                           @Valid PublicacionRequest req) {
        Publicacion p = obtenerPropia(publicacionId, vendedorId);
        if (p.getEstado() == EstadoPublicacion.VENDIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede editar una publicación marcada como vendida");
        }
        aplicarCampos(p, req);
        return PublicacionResponse.from(publicacionRepo.save(p));
    }

    @Transactional
    public PublicacionResponse cambiarEstado(Long publicacionId, Long vendedorId,
                                              EstadoPublicacion nuevoEstado) {
        Publicacion p = obtenerPropia(publicacionId, vendedorId);
        if (nuevoEstado == EstadoPublicacion.ELIMINADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Usa DELETE para eliminar una publicación");
        }
        p.setEstado(nuevoEstado);
        return PublicacionResponse.from(publicacionRepo.save(p));
    }

    @Transactional
    public void eliminar(Long publicacionId, Long vendedorId) {
        Publicacion p = obtenerPropia(publicacionId, vendedorId);
        p.setEstado(EstadoPublicacion.ELIMINADA);
        publicacionRepo.save(p);
    }

    // ─── Admin ──────────────────────────────────────────────────

    public List<PublicacionResponse> listarTodasAdmin() {
        return publicacionRepo.findByEstadoNotOrderByCreadoEnDesc(EstadoPublicacion.ELIMINADA)
                .stream().map(PublicacionResponse::from).toList();
    }

    @Transactional
    public void eliminarAdmin(Long publicacionId) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Publicación no encontrada"));
        p.setEstado(EstadoPublicacion.ELIMINADA);
        publicacionRepo.save(p);
    }

    // ─── Helpers internos ────────────────────────────────────────

    private void aplicarCampos(Publicacion p, PublicacionRequest req) {
        p.setTitulo(req.titulo());
        p.setDescripcion(req.descripcion());
        p.setPrecio(req.precio());
        p.setCategoria(req.categoria());
        p.setContacto(req.contacto());
    }

    private Publicacion obtenerPropia(Long publicacionId, Long vendedorId) {
        Publicacion p = publicacionRepo.findById(publicacionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Publicación no encontrada"));
        if (!p.getVendedorId().equals(vendedorId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso sobre esta publicación");
        }
        return p;
    }

    /**
     * Obtiene el ID de la propiedad principal del usuario.
     * Retorna null si no tiene ninguna asignada.
     */
    private Long propiedadPrincipalId(Long usuarioId) {
        if (usuarioId == null) return null;
        return usuarioPropiedadRepo.findByUsuarioId(usuarioId).stream()
                .filter(up -> up.isEsPrincipal())
                .findFirst()
                .map(up -> up.getPropiedadId())
                // Si no tiene principal, tomar la primera disponible
                .orElseGet(() -> usuarioPropiedadRepo.findByUsuarioId(usuarioId).stream()
                        .findFirst()
                        .map(up -> up.getPropiedadId())
                        .orElse(null));
    }

    /**
     * Construye la cadena de ancestros de una propiedad desde ella misma hasta la raíz.
     * Ej: [apartamento_id, piso_id, torre_id, conjunto_id]
     * Si propiedadId es null, retorna lista vacía.
     */
    private List<Long> construirCadena(Long propiedadId) {
        List<Long> cadena = new ArrayList<>();
        Long actual = propiedadId;
        // Límite de seguridad para evitar ciclos
        int limite = 20;
        while (actual != null && limite-- > 0) {
            cadena.add(actual);
            Long finalActual = actual;
            actual = propiedadRepo.findById(finalActual)
                    .map(Propiedad::getParentId)
                    .orElse(null);
        }
        return cadena;
    }

    /**
     * Calcula la distancia de árbol entre dos propiedades usando sus cadenas de ancestros.
     * Distancia 0 = misma propiedad, 1 = mismo padre (mismo piso), 2 = mismo abuelo (misma torre), etc.
     * Si no comparten ancestros o alguna cadena está vacía, retorna Integer.MAX_VALUE.
     */
    private int calcularDistancia(List<Long> cadenaComprador, List<Long> cadenaVendedor) {
        if (cadenaComprador.isEmpty() || cadenaVendedor.isEmpty()) return Integer.MAX_VALUE;

        Set<Long> ancestrosComprador = new HashSet<>(cadenaComprador);

        for (int nivelVendedor = 0; nivelVendedor < cadenaVendedor.size(); nivelVendedor++) {
            Long ancestro = cadenaVendedor.get(nivelVendedor);
            if (ancestrosComprador.contains(ancestro)) {
                int nivelComprador = cadenaComprador.indexOf(ancestro);
                // Distancia total = suma de niveles hasta el ancestro común
                return nivelComprador + nivelVendedor;
            }
        }
        return Integer.MAX_VALUE;
    }
}
