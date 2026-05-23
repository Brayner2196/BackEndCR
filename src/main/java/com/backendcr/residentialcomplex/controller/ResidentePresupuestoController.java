package com.backendcr.residentialcomplex.controller;

import com.backendcr.residentialcomplex.dto.presupuesto.PresupuestoResponse;
import com.backendcr.residentialcomplex.service.PresupuestoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/residente/presupuestos")
@PreAuthorize("isAuthenticated()")
@RequiredArgsConstructor
public class ResidentePresupuestoController {

    private final PresupuestoService presupuestoService;

    /** Presupuesto activo con categorías y ejecución (sin gastos individuales) */
    @GetMapping("/activo")
    public PresupuestoResponse obtenerActivo() {
        return presupuestoService.obtenerActivo();
    }

    /** Lista todos los presupuestos en resumen (para historial) */
    @GetMapping
    public List<PresupuestoResponse> listar() {
        return presupuestoService.listarTodos();
    }
}
