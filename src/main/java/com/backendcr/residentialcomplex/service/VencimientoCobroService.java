package com.backendcr.residentialcomplex.service;

import com.backendcr.residentialcomplex.config.TenantClock;
import com.backendcr.residentialcomplex.entity.Cobro;
import com.backendcr.residentialcomplex.entity.enums.EstadoCobro;
import com.backendcr.residentialcomplex.repository.CobroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Lógica reutilizable de vencimiento de cobros.
 *
 * <p>Detecta cobros PENDIENTE/PARCIAL cuya fecha límite de pago ya pasó (en la zona
 * del tenant) y los marca como VENCIDO <b>persistiendo el cambio en BD</b>. Solo cambia
 * el estado; el cálculo de la mora lo hace el job programado
 * {@code CobroService.calcularMoras}, que reprocesa los VENCIDO a los que aún les falta.</p>
 *
 * <p>Se usa en dos lugares:</p>
 * <ul>
 *   <li><b>Lecturas</b> (persistencia perezosa): al listar/consultar cobros se corrige el
 *       estado en BD en el momento, en vez de solo alterarlo en la respuesta.</li>
 *   <li><b>Job de mora</b>: comparte el mismo criterio de vencimiento.</li>
 * </ul>
 *
 * <p>Usa {@link Propagation#REQUIRES_NEW} para poder escribir aunque la lectura corra
 * dentro de una transacción {@code readOnly} (p. ej. el historial paginado).</p>
 */
@Service
@RequiredArgsConstructor
public class VencimientoCobroService {

    private final CobroRepository cobroRepo;

    /**
     * Marca en BD como VENCIDO los cobros de la colección que estén PENDIENTE/PARCIAL y
     * cuya fecha límite ya pasó. Muta los objetos recibidos (para que el mapeo posterior
     * a response refleje el nuevo estado) y persiste el cambio.
     *
     * @param cobros cobros ya cargados; internamente se filtran los que aplican.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarVencidos(Collection<Cobro> cobros) {
        if (cobros == null || cobros.isEmpty()) return;
        LocalDate hoy = TenantClock.hoy();
        List<Cobro> aVencer = cobros.stream()
                .filter(VencimientoCobroService::esVencible)
                .filter(c -> c.getFechaLimitePago() != null && c.getFechaLimitePago().isBefore(hoy))
                .toList();
        if (aVencer.isEmpty()) return;
        aVencer.forEach(c -> c.setEstado(EstadoCobro.VENCIDO));
        cobroRepo.saveAll(aVencer);
    }

    /** Un cobro es "vencible" si sigue activo: PENDIENTE o PARCIAL. */
    private static boolean esVencible(Cobro c) {
        return c.getEstado() == EstadoCobro.PENDIENTE || c.getEstado() == EstadoCobro.PARCIAL;
    }
}
