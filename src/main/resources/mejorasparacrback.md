# Plan de Mejoras - BackEndCR (Residential Complex Multitenant)

> Fecha: 2026-03-18
> Estado actual: Autenticación JWT multitenant + gestión básica de usuarios + creación de tenants

---

## Resumen del Estado Actual

El backend cuenta con:
- Arquitectura multitenant por esquema PostgreSQL (Hibernate schema-based multitenancy)
- Autenticación JWT con soporte multi-tenant y selección de conjunto
- Roles definidos: `SUPER_ADMIN`, `TENANT_ADMIN`, `RESIDENTE`, `RESIDENTE_PENDIENTE`, `VIGILANTE`, `PORTERO`, `PISCINERO`, `CONTADOR`
- CRUD básico de usuarios por tenant
- Creación y listado de tenants (solo SUPER_ADMIN)
- Manejo global de excepciones

---

## FASE 1 — Consolidación del Núcleo (Prioridad Alta)

### Paso 1.1 — Completar la Entidad `Usuario`

**Problema:** La entidad `Usuario` solo tiene `id`, `nombre`, `email`. El `DataInitializer` crea campos como `apto`, `torre`, `estado` pero no existen en la entidad real.

**Implementar:**
```
entity/Usuario.java
  + apto        (String)  — número de apartamento
  + torre       (String)  — torre o bloque (nullable)
  + telefono    (String)  — teléfono de contacto
  + estado      (Enum: ACTIVO, INACTIVO, PENDIENTE, SUSPENDIDO)
  + creadoEn    (LocalDateTime) — @CreationTimestamp
  + actualizadoEn (LocalDateTime) — @UpdateTimestamp
```

**Archivos a crear/modificar:**
- `entity/Usuario.java`
- `entity/enums/EstadoUsuario.java` (enum)
- `service/UsuarioService.java` (ajustar lógica)
- `controller/UsuarioController.java` (ajustar DTOs)

---

### Paso 1.2 — DTOs para Usuario

**Problema:** El controller recibe y devuelve la entidad directamente. Expone campos internos y no valida correctamente.

**Implementar:**
```
dto/usuario/
  CrearUsuarioRequest.java   — nombre, email, password, apto, torre, telefono, rol
  ActualizarUsuarioRequest.java — nombre, apto, torre, telefono, estado
  UsuarioResponse.java       — id, nombre, email, apto, torre, estado, creadoEn (sin password)
```

---

### Paso 1.3 — Creación del TENANT_ADMIN al crear un Tenant

**Problema:** Cuando se crea un tenant, no se crea automáticamente el administrador del mismo. Queda huérfano.

**Implementar en `TenantService.crearTenant()`:**
1. Crear el schema y las tablas (ya está)
2. Crear automáticamente un `Identidad` con rol `TENANT_ADMIN` para ese tenant
3. Crear el `Usuario` perfil en el nuevo schema
4. Retornar las credenciales iniciales del admin en la respuesta

**Modificar:**
- `tenant/dto/CrearTenantRequest.java` → agregar `emailAdmin` y `passwordAdmin`
- `tenant/service/TenantService.java` → lógica de creación del admin
- `tenant/dto/CrearTenantResponse.java` → nuevo DTO con info del tenant + admin creado

---

### Paso 1.4 — Auto-registro de Residentes

**Problema:** No hay forma de que un nuevo residente se registre sin que un admin lo cree manualmente.

**Implementar endpoint público:**
```
POST /auth/registro
  Body: nombre, email, password, tenantId (código del conjunto), apto, torre, telefono
  → Crea Identidad con rol RESIDENTE_PENDIENTE
  → Crea Usuario en el schema del tenant
  → Responde con mensaje de espera de aprobación
```

**Archivos a crear:**
- `auth/RegistroRequest.java`
- `auth/RegistroResponse.java`
- Lógica en `AuthService.java`
- Endpoint en `AuthController.java`

---

### Paso 1.5 — Aprobación de Residentes Pendientes

**Quién puede:** `TENANT_ADMIN`

**Implementar:**
```
GET  /api/usuarios/pendientes          → lista residentes con estado PENDIENTE
PUT  /api/usuarios/{id}/aprobar        → cambia rol a RESIDENTE, estado a ACTIVO
PUT  /api/usuarios/{id}/rechazar       → elimina o marca como RECHAZADO
```

---

## FASE 2 — Módulo de Apartamentos/Unidades (Prioridad Alta)

### Paso 2.1 — Entidad `Apartamento`

Cada conjunto tiene apartamentos. Los residentes están asociados a un apartamento.

```
entity/Apartamento.java (en schema del tenant)
  + id          (Long)
  + numero      (String)  — "101", "B-202"
  + torre       (String)  — nullable
  + piso        (Integer) — nullable
  + estado      (Enum: OCUPADO, DISPONIBLE, EN_MORA)
  + propietario (String)  — nombre del propietario (puede ser diferente al residente)
  + creadoEn    (LocalDateTime)
```

### Paso 2.2 — Relación Usuario ↔ Apartamento

- Un apartamento puede tener múltiples `RESIDENTE` (familia)
- Un `RESIDENTE` pertenece a un `Apartamento`
- Agregar `apartamento` (FK) a `Usuario`

### Paso 2.3 — CRUD de Apartamentos

```
GET    /api/apartamentos               → lista todos (TENANT_ADMIN)
GET    /api/apartamentos/{id}          → detalle (TENANT_ADMIN, RESIDENTE propio)
POST   /api/apartamentos              → crear (TENANT_ADMIN)
PUT    /api/apartamentos/{id}          → actualizar (TENANT_ADMIN)
DELETE /api/apartamentos/{id}          → desactivar (TENANT_ADMIN)
GET    /api/apartamentos/{id}/residentes → residentes del apto (TENANT_ADMIN)
```

---

## FASE 3 — Módulo de Personal (Prioridad Media-Alta)

### Paso 3.1 — CRUD de Personal del Conjunto

El `TENANT_ADMIN` debe poder crear y gestionar: `VIGILANTE`, `PORTERO`, `PISCINERO`, `CONTADOR`.

**Implementar en `UsuarioController`:**
```
POST /api/personal                     → crear personal (TENANT_ADMIN)
  Body: nombre, email, password, rol (VIGILANTE|PORTERO|PISCINERO|CONTADOR), telefono
GET  /api/personal                     → listar personal (TENANT_ADMIN)
PUT  /api/personal/{id}                → actualizar (TENANT_ADMIN)
PUT  /api/personal/{id}/desactivar     → desactivar acceso (TENANT_ADMIN)
```

**Lógica:**
1. Crear `Identidad` con rol correspondiente y `tenantId`
2. Crear `Usuario` perfil en el schema
3. Enviar credenciales (o retornarlas en la respuesta para el admin)

---

## FASE 4 — Módulo de Visitantes (Prioridad Media)

### Paso 4.1 — Entidad `Visita`

Registro de entrada/salida de visitantes. Lo gestiona `PORTERO` o `VIGILANTE`.

```
entity/Visita.java (en schema del tenant)
  + id              (Long)
  + nombreVisitante (String)
  + documentoId     (String)
  + apartamentoId   (Long, FK)
  + motivoVisita    (String)
  + fechaEntrada    (LocalDateTime)
  + fechaSalida     (LocalDateTime, nullable)
  + registradoPor   (Long) — id del portero/vigilante
  + estado          (Enum: ACTIVA, FINALIZADA)
```

### Paso 4.2 — Endpoints de Visitas

```
POST /api/visitas                      → registrar entrada (PORTERO, VIGILANTE)
PUT  /api/visitas/{id}/salida          → registrar salida (PORTERO, VIGILANTE)
GET  /api/visitas                      → historial (TENANT_ADMIN, PORTERO, VIGILANTE)
GET  /api/visitas/activas              → visitas en curso (PORTERO, VIGILANTE)
GET  /api/visitas/apartamento/{id}     → visitas por apartamento (TENANT_ADMIN, RESIDENTE propio)
```

---

## FASE 5 — Módulo de Reservas de Zonas Comunes (Prioridad Media)

### Paso 5.1 — Entidad `ZonaComun`

```
entity/ZonaComun.java (en schema del tenant)
  + id          (Long)
  + nombre      (String)  — "Piscina", "Salón Comunal", "BBQ", "Cancha"
  + capacidad   (Integer)
  + activa      (boolean)
  + horaApertura (LocalTime)
  + horaCierre   (LocalTime)
```

### Paso 5.2 — Entidad `Reserva`

```
entity/Reserva.java (en schema del tenant)
  + id              (Long)
  + zonaComun       (ZonaComun, FK)
  + usuario         (Usuario, FK)  — residente que reserva
  + fechaReserva    (LocalDate)
  + horaInicio      (LocalTime)
  + horaFin         (LocalTime)
  + estado          (Enum: PENDIENTE, CONFIRMADA, CANCELADA)
  + creadoEn        (LocalDateTime)
```

### Paso 5.3 — Endpoints de Reservas

```
GET  /api/zonas-comunes                → listar zonas (todos los roles autenticados)
POST /api/zonas-comunes                → crear zona (TENANT_ADMIN)
PUT  /api/zonas-comunes/{id}           → actualizar zona (TENANT_ADMIN)

POST /api/reservas                     → crear reserva (RESIDENTE)
GET  /api/reservas/mis-reservas        → mis reservas (RESIDENTE)
GET  /api/reservas                     → todas las reservas (TENANT_ADMIN, PISCINERO)
PUT  /api/reservas/{id}/confirmar      → confirmar (TENANT_ADMIN, PISCINERO)
PUT  /api/reservas/{id}/cancelar       → cancelar (RESIDENTE propio, TENANT_ADMIN)
```

---

## FASE 6 — Módulo de Pagos y Cuotas (Prioridad Media)

### Paso 6.1 — Entidad `CuotaAdministracion`

```
entity/CuotaAdministracion.java (en schema del tenant)
  + id              (Long)
  + apartamento     (Apartamento, FK)
  + periodo         (String)  — "2025-03" (año-mes)
  + monto           (BigDecimal)
  + fechaVencimiento (LocalDate)
  + estado          (Enum: PENDIENTE, PAGADA, VENCIDA, EN_MORA)
  + creadoEn        (LocalDateTime)
```

### Paso 6.2 — Entidad `PagoCuota`

```
entity/PagoCuota.java (en schema del tenant)
  + id              (Long)
  + cuota           (CuotaAdministracion, FK)
  + fechaPago       (LocalDateTime)
  + montoPagado     (BigDecimal)
  + metodoPago      (Enum: EFECTIVO, TRANSFERENCIA, PSE, OTRO)
  + comprobante     (String)  — número de comprobante/referencia
  + registradoPor   (Long)   — id del contador
```

### Paso 6.3 — Endpoints de Pagos

```
POST /api/cuotas/generar-mensual       → generar cuotas para todos los aptos (TENANT_ADMIN, CONTADOR)
GET  /api/cuotas                       → listar todas (TENANT_ADMIN, CONTADOR)
GET  /api/cuotas/mi-estado             → estado de mis cuotas (RESIDENTE)
POST /api/cuotas/{id}/pago             → registrar pago (CONTADOR)
GET  /api/cuotas/morosos               → apartamentos en mora (TENANT_ADMIN, CONTADOR)
```

---

## FASE 7 — Módulo de Comunicados (Prioridad Media)

### Paso 7.1 — Entidad `Comunicado`

```
entity/Comunicado.java (en schema del tenant)
  + id          (Long)
  + titulo      (String)
  + contenido   (String/Text)
  + tipo        (Enum: CIRCULAR, URGENTE, INFORMATIVO, CITACION)
  + publicadoPor (Long)  — id del TENANT_ADMIN
  + fechaPublicacion (LocalDateTime)
  + activo      (boolean)
```

### Paso 7.2 — Endpoints de Comunicados

```
POST /api/comunicados                  → publicar (TENANT_ADMIN)
GET  /api/comunicados                  → listar activos (todos los roles del conjunto)
GET  /api/comunicados/{id}             → detalle (todos los roles del conjunto)
PUT  /api/comunicados/{id}             → editar (TENANT_ADMIN)
DELETE /api/comunicados/{id}           → desactivar (TENANT_ADMIN)
```

---

## FASE 8 — Módulo de PQRS (Prioridad Media-Baja)

### Paso 8.1 — Entidad `Pqrs`

```
entity/Pqrs.java (en schema del tenant)
  + id              (Long)
  + tipo            (Enum: PETICION, QUEJA, RECLAMO, SUGERENCIA)
  + titulo          (String)
  + descripcion     (String/Text)
  + usuario         (Usuario, FK)  — quien la crea
  + estado          (Enum: ABIERTA, EN_REVISION, RESUELTA, CERRADA)
  + respuesta       (String/Text, nullable)
  + respondidoPor   (Long, nullable)
  + creadoEn        (LocalDateTime)
  + actualizadoEn   (LocalDateTime)
```

### Paso 8.2 — Endpoints de PQRS

```
POST /api/pqrs                         → crear PQRS (RESIDENTE)
GET  /api/pqrs/mis-pqrs                → mis solicitudes (RESIDENTE)
GET  /api/pqrs                         → todas (TENANT_ADMIN)
PUT  /api/pqrs/{id}/responder          → responder y cerrar (TENANT_ADMIN)
```

---

## FASE 9 — Mejoras de Seguridad y Robustez (Transversal)

### Paso 9.1 — Cambio de Contraseña

```
PUT /auth/cambiar-password
  Headers: Authorization: Bearer <token>
  Body: passwordActual, passwordNueva, confirmacionPassword
```

### Paso 9.2 — Refresh Token

Implementar un sistema de refresh token para no forzar re-login al expirar el JWT.

```
entity/RefreshToken.java (en schema public)
  + id          (Long)
  + token       (String, UUID)
  + identidadId (Long, FK)
  + expiresAt   (LocalDateTime)
  + usado       (boolean)

POST /auth/refresh-token
  Body: refreshToken
  → Valida token → Genera nuevo JWT + nuevo refreshToken
```

### Paso 9.3 — Externalizar Secrets de Configuración

- Mover el JWT secret a variables de entorno (`JWT_SECRET`)
- Usar `@Value("${JWT_SECRET}")` en `JwtService`
- Documentar en `.env.example`

### Paso 9.4 — Auditoría de Acciones

```
entity/AuditoriaLog.java (en schema del tenant o public)
  + id          (Long)
  + accion      (String)  — "CREAR_USUARIO", "APROBAR_RESIDENTE", etc.
  + entidad     (String)
  + entidadId   (Long)
  + realizadoPor (Long)
  + tenantId    (String)
  + detalles    (String/JSON)
  + fecha       (LocalDateTime)
```

Implementar como un `@Aspect` (Spring AOP) que intercepte métodos de servicio marcados con `@Auditable`.

---

## FASE 10 — Mejoras al Schema Multitenant

### Paso 10.1 — Migración de Schemas con Flyway/Liquibase

Reemplazar el `CREATE TABLE` manual en `TenantService` con scripts de migración versionados.

**Dependencia a agregar en `pom.xml`:**
```xml
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

**Estructura:**
```
resources/db/migration/
  V1__create_usuarios.sql
  V2__create_apartamentos.sql
  V3__create_visitas.sql
  ...
```

### Paso 10.2 — Soft Delete para Tenants

Agregar `fechaDesactivacion` a `Tenant` y un endpoint `DELETE /api/tenants/{id}` que solo desactive (no elimine en base de datos).

---

## Orden Recomendado de Implementación

| Prioridad | Fase | Descripción | Motivo |
|-----------|------|-------------|--------|
| 1 | 1.1 + 1.2 | Completar entidad Usuario + DTOs | Base de todo lo demás |
| 2 | 1.3 | Admin al crear tenant | Flujo crítico incompleto |
| 3 | 1.4 + 1.5 | Auto-registro + aprobación residentes | Flujo de negocio principal |
| 4 | 2.x | Módulo de Apartamentos | Necesario para pagos y visitas |
| 5 | 3.x | CRUD de Personal | Necesario para visitas y reservas |
| 6 | 4.x | Módulo de Visitantes | Uso diario (portero/vigilante) |
| 7 | 5.x | Reservas de Zonas Comunes | Alto valor para el residente |
| 8 | 6.x | Módulo de Pagos | Alto valor para administración |
| 9 | 9.x | Mejoras de seguridad | Buenas prácticas antes de producción |
| 10 | 7.x | Comunicados | Comunicación interna |
| 11 | 8.x | PQRS | Mejora de servicio al residente |
| 12 | 10.x | Flyway + Soft Delete | Mejora arquitectural |

---

## Notas Adicionales

- Todos los endpoints de tenant (excepto `/auth/**`) deben incluir el header `X-Tenant-ID`
- Los residentes solo deben acceder a sus propios datos (apartamento, cuotas, PQRS propias)
- Considerar agregar paginación (`Pageable`) en todos los listados desde la Fase 2
- El `TENANT_ADMIN` solo opera dentro de su propio tenant (validar en `TenantFilter` o en los servicios)
- Los enums (roles, estados) deberían centralizarse en un paquete `entity/enums/`
