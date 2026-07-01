package com.backendcr.residentialcomplex.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.backendcr.residentialcomplex.entity.Propiedad;
import com.backendcr.residentialcomplex.repository.PropiedadRepository;

import lombok.RequiredArgsConstructor;

/**
 * Logica reutilizable para el "path corto" de una propiedad (concatenacion de
 * los identificadores desde la raiz hasta la hoja).
 *
 * <p>Centraliza la REGLA de concatenacion en un unico metodo ({@link #concat})
 * para que escritura, backfill y verificacion produzcan siempre el mismo valor.
 * Actualmente sin separador (ej. "A" + "1" + "01" => "A101"); cambiar la regla
 * aqui es suficiente para cambiarla en toda la app.</p>
 */
@Component
@RequiredArgsConstructor
public class PropiedadPathCalculator {

    private final PropiedadRepository propiedadRepo;

    /** Regla unica de concatenacion padre+hijo. Cambiar aqui = cambiar en todo. */
    public String concat(String prefijoPadre, String identificador) {
        String p = prefijoPadre == null ? "" : prefijoPadre;
        String id = identificador == null ? "" : identificador;
        return p + id;
    }

    /**
     * Calcula el path corto recorriendo hacia arriba (raiz -> hoja).
     * Fuente de verdad / fallback cuando la columna aun no esta poblada.
     */
    public String construirPathCorto(Propiedad hoja) {
        List<String> partes = new ArrayList<>();
        Propiedad actual = hoja;
        while (actual != null) {
            partes.add(0, actual.getIdentificador());
            if (actual.getParentId() == null) break;
            actual = propiedadRepo.findById(actual.getParentId()).orElse(null);
        }
        String path = "";
        for (String parte : partes) {
            path = concat(path, parte);
        }
        return path;
    }

    /**
     * Devuelve el path corto persistido; si esta vacio lo calcula (fallback).
     * No persiste: usar en lecturas puntuales.
     */
    public String pathCortoDe(Propiedad p) {
        if (p == null) return null;
        if (p.getPathCorto() != null && !p.getPathCorto().isBlank()) {
            return p.getPathCorto();
        }
        return construirPathCorto(p);
    }

    /**
     * Recalcula y persiste el path corto del nodo y de TODOS sus descendientes.
     * Necesario cuando cambia el identificador de un nodo intermedio o cuando se
     * mueve (cambia parentId). Recorre hacia abajo acumulando el prefijo, sin
     * N+1 por nivel.
     */
    @Transactional
    public void recalcularSubarbol(Long nodoId) {
        Propiedad nodo = propiedadRepo.findById(nodoId).orElse(null);
        if (nodo == null) return;

        String prefijoPadre = "";
        if (nodo.getParentId() != null) {
            Propiedad padre = propiedadRepo.findById(nodo.getParentId()).orElse(null);
            prefijoPadre = padre != null ? pathCortoDe(padre) : "";
        }
        recalcularDesde(nodo, prefijoPadre);
    }

    /** Setea el path del nodo (prefijo + su identificador) y recursa en hijos. */
    private void recalcularDesde(Propiedad nodo, String prefijoPadre) {
        String nuevo = concat(prefijoPadre, nodo.getIdentificador());
        if (nuevo.equals(nodo.getPathCorto())) {
            // El nodo ya esta bien; aun asi hay que revisar hijos por si cambiaron.
        } else {
            nodo.setPathCorto(nuevo);
            propiedadRepo.save(nodo);
        }
        for (Propiedad hijo : propiedadRepo.findByParentId(nodo.getId())) {
            recalcularDesde(hijo, nuevo);
        }
    }
}
