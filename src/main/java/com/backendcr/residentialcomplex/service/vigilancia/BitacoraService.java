package com.backendcr.residentialcomplex.service.vigilancia;

import com.backendcr.residentialcomplex.dto.vigilancia.BitacoraAccesoResponse;
import com.backendcr.residentialcomplex.entity.BitacoraAcceso;
import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.entity.enums.ResultadoAcceso;
import com.backendcr.residentialcomplex.entity.enums.TipoEventoAcceso;
import com.backendcr.residentialcomplex.repository.BitacoraAccesoRepository;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Minuta de vigilancia: registra y consulta eventos de portería.
 */
@Service
@RequiredArgsConstructor
public class BitacoraService {

    private final BitacoraAccesoRepository bitacoraRepo;
    private final PropiedadRepository propiedadRepo;

    /** Inserta un evento en la minuta. Usado por los demás servicios de vigilancia. */
    @Transactional
    public BitacoraAcceso registrar(TipoEventoAcceso tipo, ResultadoAcceso resultado,
                                    String descripcion, Long propiedadId, String placa,
                                    String documento, String nombreVisitante,
                                    Long vigilanteId, Long visitaId, Long paqueteId) {
        BitacoraAcceso b = new BitacoraAcceso();
        b.setTipoEvento(tipo);
        b.setResultado(resultado);
        b.setDescripcion(descripcion);
        b.setPropiedadId(propiedadId);
        b.setPlaca(placa);
        b.setDocumento(documento);
        b.setNombreVisitante(nombreVisitante);
        b.setVigilanteId(vigilanteId);
        b.setVisitaId(visitaId);
        b.setPaqueteId(paqueteId);
        return bitacoraRepo.save(b);
    }

    public List<BitacoraAccesoResponse> recientes(int limite) {
        return bitacoraRepo.findAllByOrderByCreadoEnDesc(PageRequest.of(0, Math.min(limite, 200)))
                .stream().map(this::toResponse).toList();
    }

    public List<BitacoraAccesoResponse> porRango(Instant desde, Instant hasta) {
        return bitacoraRepo.findAllByCreadoEnBetweenOrderByCreadoEnDesc(desde, hasta)
                .stream().map(this::toResponse).toList();
    }

    private BitacoraAccesoResponse toResponse(BitacoraAcceso b) {
        String ident = b.getPropiedadId() == null ? null
                : propiedadRepo.findById(b.getPropiedadId()).map(Propiedad::getIdentificador).orElse(null);
        return BitacoraAccesoResponse.from(b, ident);
    }
}
