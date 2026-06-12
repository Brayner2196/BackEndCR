package com.backendcr.residentialcomplex.service.cartera;

import com.backendcr.residentialcomplex.entity.CondicionRegla;
import com.backendcr.residentialcomplex.entity.ReglaEstadoCartera;
import com.backendcr.residentialcomplex.entity.enums.OperadorLogico;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Evalúa si una regla de cartera se cumple para unas métricas dadas.
 * Lógica separada y sin estado → fácilmente testeable de forma unitaria.
 */
@Component
public class EvaluadorCondiciones {

    /**
     * @return true si la regla se cumple. AND = todas sus condiciones; OR = alguna.
     *         Una regla sin condiciones nunca se cumple (evita bloqueos accidentales).
     */
    public boolean cumpleRegla(ReglaEstadoCartera regla,
                               List<CondicionRegla> condiciones,
                               MetricasCartera metricas) {
        if (condiciones == null || condiciones.isEmpty()) return false;

        boolean esAnd = regla.getOperadorLogico() == OperadorLogico.AND;
        for (CondicionRegla c : condiciones) {
            boolean ok = c.getOperador().evaluar(metricas.valor(c.getCampo()), c.getValor());
            if (esAnd && !ok) return false;   // AND: una falla → falla todo
            if (!esAnd && ok) return true;    // OR: una pasa → pasa
        }
        return esAnd; // AND llegó sin fallar = true; OR sin aciertos = false
    }
}
