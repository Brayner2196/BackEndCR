package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.cartera.EstadoCarteraResponse;
import com.backendcr.residentialcomplex.service.cartera.EvaluadorCarteraService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Consulta y recálculo del estado de cartera de una propiedad (admin).
 * La configuración de estados/reglas/restricciones (CRUD) es Fase 2.
 */
@RestController
@RequestMapping("/api/admin/propiedades")
@PreAuthorize("hasRole('TENANT_ADMIN')")
@RequiredArgsConstructor
public class CarteraController {

    private final EvaluadorCarteraService evaluador;

    /** Estado de cartera vigente de todas las propiedades (para badges en listas). */
    @GetMapping("/estados-cartera")
    public List<EstadoCarteraResponse> listarEstados() {
        return evaluador.listarEstadosVigentes();
    }

    @GetMapping("/{id}/estado-cartera")
    public EstadoCarteraResponse consultar(@PathVariable Long id) {
        return evaluador.consultarEstado(id);
    }

    @PostMapping("/{id}/estado-cartera/recalcular")
    public EstadoCarteraResponse recalcular(@PathVariable Long id) {
        return evaluador.recalcularYConsultar(id);
    }
}
