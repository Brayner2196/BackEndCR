# Sistema de Estados de Cartera por Propiedad

Diseño de un sistema **configurable por cada conjunto residencial (tenant)** que
determina el estado financiero de una propiedad (al día, vencida, en mora,
cartera, cobro prejurídico, jurídico…), bajo qué reglas se entra a cada estado y
qué **restricciones** aplica ese estado (no reservar, no ingresar el vehículo,
no descargar paz y salvo, etc.).

> Versión 1.0 · Núcleo implementado. Las secciones marcadas como _Fase 2/3_ son
> extensiones previstas pero no incluidas en esta entrega.

---

## 1. Principios de diseño

1. **Eje independiente.** El estado de cartera es un eje **nuevo y ortogonal** al
   `EstadoPropiedad` actual (DISPONIBLE/OCUPADA/DESOCUPADA, que es de _ocupación_).
   No se mezclan: una propiedad puede estar `OCUPADA` y a la vez `EN_MORA`.
2. **Configurable por tenant.** Cada conjunto define sus propios estados, reglas y
   restricciones. El esquema multitenant (schema-per-tenant) ya aísla las tablas,
   así que la configuración vive en el schema del tenant sin `tenant_id`.
3. **Derivado, no manual.** El estado se **calcula** a partir de los cobros de la
   propiedad mediante un motor de reglas. Se persiste un _snapshot_ para consultas
   rápidas y se mantiene historial de cambios.
4. **Punto único de aplicación.** Ningún feature decide por su cuenta si bloquea.
   Todos consultan un único `RestriccionService.verificar(propiedad, acción)`.
5. **Degradación segura.** Si un tenant no configuró estados/reglas, el sistema
   **no bloquea nada** (todo permitido). Las restricciones son _opt-in_.
6. **Motor híbrido.** Reglas simples por umbral (días + monto) para el 90% de los
   casos, con posibilidad de condiciones componibles (AND/OR) para casos complejos.

---

## 2. Modelo de datos

Seis tablas nuevas (todas dentro del schema del tenant):

```
estados_cartera ──< reglas_estado_cartera ──< condiciones_regla
       │
       └──< restricciones_estado

estado_cartera_propiedad   (snapshot vigente, 1:1 con propiedad)
historial_estado_cartera   (auditoría de transiciones)
```

### 2.1 `estados_cartera` — catálogo configurable
El admin define los estados de su conjunto.

| Campo          | Tipo     | Notas |
|----------------|----------|-------|
| id             | bigint   | PK |
| codigo         | varchar  | Slug único: `AL_DIA`, `VENCIDA`, `MORA`, `COBRO_PREJURIDICO`… |
| nombre         | varchar  | Display |
| descripcion    | varchar  | Opcional |
| severidad      | int      | Mayor = más grave. Desempata cuando varias reglas aplican |
| color          | varchar  | Hex para la UI (badges) |
| es_positivo    | boolean  | `AL_DIA` = true; el resto false |
| activo         | boolean  | |

Regla de oro: **siempre existe un estado base `es_positivo=true`** (al día) que se
asigna cuando ninguna regla negativa se cumple.

### 2.2 `reglas_estado_cartera` — condiciones de entrada
Un estado puede tener **varias reglas**; basta que **una** se cumpla para entrar
(las reglas se evalúan en OR entre sí). Dentro de una regla, sus condiciones se
combinan con `operador_logico`.

| Campo            | Tipo    | Notas |
|------------------|---------|-------|
| id               | bigint  | PK |
| estado_cartera_id| bigint  | FK |
| nombre           | varchar | Ej: "Más de 60 días y más de $500k" |
| operador_logico  | enum    | `AND` / `OR` (combina sus condiciones) |
| orden            | int     | |
| activa           | boolean | |

### 2.3 `condiciones_regla` — criterios atómicos
El corazón del motor híbrido. Una condición compara un **campo de cartera** con un
**valor** usando un **operador**.

| Campo      | Tipo    | Notas |
|------------|---------|-------|
| id         | bigint  | PK |
| regla_id   | bigint  | FK |
| campo      | enum    | `DIAS_VENCIDO_MAX`, `MONTO_ADEUDADO`, `NUM_PERIODOS_VENCIDOS`, `NUM_COBROS_VENCIDOS` |
| operador   | enum    | `MAYOR_QUE`, `MAYOR_IGUAL`, `MENOR_QUE`, `MENOR_IGUAL`, `IGUAL`, `DIFERENTE` |
| valor      | decimal | Umbral a comparar |

**Umbral simple** = una regla con una condición (`DIAS_VENCIDO_MAX >= 30`).
**Componible** = una regla `AND` con varias condiciones, o varias reglas en OR.

### 2.4 `restricciones_estado` — efectos/prohibiciones
Qué bloquea cada estado. El admin marca, por estado, qué acciones se prohíben.

| Campo            | Tipo    | Notas |
|------------------|---------|-------|
| id               | bigint  | PK |
| estado_cartera_id| bigint  | FK |
| accion           | enum    | `AccionRestringible` (ver §3) |
| mensaje          | varchar | Texto mostrado al usuario/vigilante |

La sola existencia de la fila = acción bloqueada en ese estado.

### 2.5 `estado_cartera_propiedad` — snapshot vigente
Evita recalcular en cada consulta. 1:1 con propiedad.

| Campo             | Tipo      | Notas |
|-------------------|-----------|-------|
| id                | bigint    | PK |
| propiedad_id      | bigint    | UNIQUE, FK lógica |
| estado_cartera_id | bigint    | FK |
| dias_vencido_max  | int       | Métrica congelada |
| monto_adeudado    | decimal   | Métrica congelada |
| calculado_en      | timestamp | |

### 2.6 `historial_estado_cartera` — auditoría
Una fila por **cambio** de estado.

| Campo               | Tipo      |
|---------------------|-----------|
| id                  | bigint    |
| propiedad_id        | bigint    |
| estado_anterior_id  | bigint    |
| estado_nuevo_id     | bigint    |
| dias_vencido_max    | int       |
| monto_adeudado      | decimal   |
| creado_en           | timestamp |

---

## 3. Catálogo de acciones restringibles

Enum `AccionRestringible` — el universo de cosas que un estado puede prohibir.
Extensible: agregar un valor no rompe nada (estados que no lo referencian no lo bloquean).

| Acción                      | Punto de aplicación |
|-----------------------------|---------------------|
| `RESERVAR_ZONA_COMUN`       | Crear reserva (ya integrado) |
| `ACCESO_VEHICULAR`          | Vigilante valida placa (caso estrella) |
| `ACCESO_PEATONAL_VISITANTE` | Vigilante autoriza visitante _(Fase 2)_ |
| `DESCARGAR_PAZ_Y_SALVO`     | Descarga de certificado _(Fase 2)_ |
| `VOTAR_ASAMBLEA`            | Módulo votaciones _(Fase 2)_ |
| `PUBLICAR_MARKETPLACE`      | Módulo marketplace _(Fase 2)_ |

---

## 4. Motor de evaluación

### 4.1 Métricas de cartera
Para una propiedad se calculan desde sus `cobros` activos (PENDIENTE/PARCIAL/VENCIDO):

- **`diasVencidoMax`** — máximo de `(hoy − fecha_limite_pago)` entre cobros vencidos.
- **`montoAdeudado`** — suma de `monto_pendiente`.
- **`numCobrosVencidos`** — cobros con fecha límite pasada y saldo pendiente.
- **`numPeriodosVencidos`** — periodos distintos con cobro vencido.

> Extensible (_Fase 2_): `diasDesdeUltimoPago`, antigüedad media, % de cumplimiento.

### 4.2 Resolución del estado
```
metricas = calcularMetricas(propiedad)
estados  = estados activos, ordenados por severidad DESC
para cada estado (de más grave a menos):
    si alguna de sus reglas se cumple → asignar y terminar
si ninguno aplica → estado base (es_positivo)
```
Gana siempre el estado **más severo** cuyas reglas se cumplan. Esto hace la config
robusta: el admin no tiene que preocuparse por solapamientos.

### 4.3 Evaluación de una regla (reutilizable)
`EvaluadorCondiciones` (lógica separada y testeable):
- Resuelve el valor de cada `campo` desde las métricas.
- Aplica el `operador` contra el `valor`.
- Combina las condiciones con el `operador_logico` (AND = todas; OR = alguna).

### 4.4 Recálculo
- **Puntual:** `recalcular(propiedadId)` tras un pago, una exoneración o un cobro nuevo.
- **Masivo:** job `@Scheduled` diario que itera tenants (mismo patrón que
  `CobroService.calcularMoras`) y recalcula todas las propiedades. Debe correr
  **después** del job de moras (que es quien marca cobros como `VENCIDO`).
- En cada cambio: actualiza el snapshot, escribe historial y (opcional) notifica.

---

## 5. Aplicación de restricciones (enforcement)

`RestriccionService` es el **único** punto de verdad:

```java
ResultadoRestriccion r = restriccionService.verificar(propiedadId, AccionRestringible.ACCESO_VEHICULAR);
if (!r.permitido()) { /* bloquear con r.mensaje() y r.estado() */ }
```

- `puede(propiedad, accion)` → boolean simple.
- `verificar(propiedad, accion)` → `ResultadoRestriccion{ permitido, estadoCodigo, estadoNombre, mensaje }`.

Lee el snapshot de la propiedad y consulta si su estado bloquea esa acción. Si no
hay snapshot o estado configurado → **permitido** (degradación segura).

---

## 6. Flujo del vigilante (caso estrella)

```
Portero escanea/teclea la placa
   → GET /api/vigilante/acceso-vehicular?placa=ABC123
   → Vehiculo(placa).propiedadId
   → RestriccionService.verificar(propiedadId, ACCESO_VEHICULAR)
   → respuesta:
       { permitido: false,
         placa: "ABC123",
         propiedad: "Torre A / Apto 501",
         estado: "EN_MORA",
         mensaje: "Acceso restringido: la unidad está en mora" }
```

El endpoint queda listo en esta entrega. La autorización por **rol VIGILANTE** se
añade cuando ese rol exista (hoy se protege como endpoint autenticado del conjunto).
La decisión final (dejar pasar o no) la toma el portero; el sistema **informa**, no
abre/cierra talanqueras (eso sería _Fase 3_ con hardware).

---

## 7. Integración con lo existente

- **Reservas:** `ReglasReservaValidator` ya valida `sinDeudaPendiente`. Se
  complementa consultando `RestriccionService.verificar(..., RESERVAR_ZONA_COMUN)`
  para respetar la configuración por estado del tenant.
- **Cobros:** el job de moras (`calcularMoras`) es el disparador natural del
  recálculo masivo de cartera.

---

## 8. Despliegue

- **dev** (`ddl-auto=update`): las tablas se crean solas al levantar.
- **prod** (`ddl-auto=none`): ejecutar el DDL en **cada schema de tenant**. Ver
  `docs/sql/cartera_estados.sql` _(a generar junto con el release)_.
- **Seed:** al activar el feature en un tenant, sembrar un set por defecto
  (`AL_DIA`, `VENCIDA`, `MORA`, `COBRO_PREJURIDICO`) con reglas de umbral típicas.
  Mientras no haya seed, el sistema no bloquea (seguro).

---

## 9. Plan por fases

| Fase | Alcance |
|------|---------|
| **1 (esta entrega)** | Modelo de datos, motor híbrido, `RestriccionService`, endpoint vigilante, recálculo + job, integración con reservas |
| **2** | UI admin de configuración (CRUD de estados/reglas/restricciones), seed por defecto, paz y salvo, votaciones/marketplace, `diasDesdeUltimoPago` |
| **3** | Notificaciones de cambio de estado, integración con hardware de acceso, panel de cartera/morosidad enriquecido |

---

## 10. Ejemplo de configuración (conjunto típico)

| Estado | Severidad | Regla de entrada | Restricciones |
|--------|-----------|------------------|---------------|
| Al día | 0 | (base) | — |
| Vencida | 10 | `DIAS_VENCIDO_MAX >= 1` | — |
| En mora | 20 | `DIAS_VENCIDO_MAX >= 30` **AND** `MONTO_ADEUDADO >= 100000` | `RESERVAR_ZONA_COMUN`, `ACCESO_VEHICULAR` |
| Prejurídico | 30 | `DIAS_VENCIDO_MAX >= 90` **OR** `NUM_PERIODOS_VENCIDOS >= 3` | todas las anteriores + `VOTAR_ASAMBLEA` |
