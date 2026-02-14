# BackEndCR - Aplicación Multitenant para Complejos Residenciales

Aplicación backend para gestionar complejos residenciales (conjuntos residenciales) con arquitectura multitenant usando Spring Boot 3.2.2 y Java 21.

## Pila Tecnológica

- **Java**: 21 (OpenJDK)
- **Spring Boot**: 3.2.2
- **Base de Datos**: H2 (en memoria)
- **ORM**: Hibernate/JPA
- **Herramienta de Construcción**: Maven
- **Arquitectura**: Multitenant con columna discriminadora

## Características

- **Arquitectura Multitenant**: Base de datos única con aislamiento de inquilinos usando columna discriminadora
- **API RESTful**: Operaciones CRUD completas para:
  - Complejos Residenciales
  - Unidades Residenciales
  - Residentes
- **Consola H2**: Interfaz web para gestión de base de datos
- **Validación**: Validación de entrada usando Bean Validation
- **Manejo de Excepciones**: Controlador global de excepciones con respuestas de error apropiadas

## Requisitos Previos

- Java 21 o superior
- Maven 3.6 o superior

## Construir el Proyecto

```bash
mvn clean install
```

## Ejecutar la Aplicación

```bash
mvn spring-boot:run
```

La aplicación se iniciará en `http://localhost:8080`

## Consola H2 Database

Accede a la consola H2 en: `http://localhost:8080/h2-console`

- **JDBC URL**: `jdbc:h2:mem:residentialdb`
- **Usuario**: `sa`
- **Contraseña**: (dejar vacío)

## Uso de Multitenant

La aplicación utiliza identificación de inquilinos basada en encabezados. Incluye el encabezado `X-Tenant-ID` en tus solicitudes:

```bash
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/complexes
```

Si no se proporciona encabezado de inquilino, el sistema utiliza un inquilino predeterminado.

## Puntos de Acceso de la API

### Complejos Residenciales

- `GET /api/complexes` - Obtener todos los complejos
- `GET /api/complexes/{id}` - Obtener complejo por ID
- `GET /api/complexes/search?name={name}` - Buscar complejos por nombre
- `POST /api/complexes` - Crear nuevo complejo
- `PUT /api/complexes/{id}` - Actualizar complejo
- `DELETE /api/complexes/{id}` - Eliminar complejo

### Unidades Residenciales

- `GET /api/units` - Obtener todas las unidades
- `GET /api/units/{id}` - Obtener unidad por ID
- `GET /api/units/complex/{complexId}` - Obtener unidades por complejo
- `POST /api/units` - Crear nueva unidad
- `PUT /api/units/{id}` - Actualizar unidad
- `DELETE /api/units/{id}` - Eliminar unidad

### Residentes

- `GET /api/residents` - Obtener todos los residentes
- `GET /api/residents/{id}` - Obtener residente por ID
- `GET /api/residents/unit/{unitId}` - Obtener residentes por unidad
- `GET /api/residents/search?lastName={lastName}` - Buscar residentes por apellido
- `POST /api/residents` - Crear nuevo residente
- `PUT /api/residents/{id}` - Actualizar residente
- `DELETE /api/residents/{id}` - Eliminar residente

## Solicitudes de Ejemplo

### Crear un Complejo Residencial

```bash
curl -X POST http://localhost:8080/api/complexes \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant1" \
  -d '{
    "name": "Conjunto Residencial Los Pinos",
    "address": "Calle 123 #45-67, Bogotá",
    "phone": "+57 300 1234567",
    "email": "info@lospinos.com",
    "totalUnits": 50
  }'
```

### Crear una Unidad Residencial

```bash
curl -X POST http://localhost:8080/api/units \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant1" \
  -d '{
    "complexId": 1,
    "unitNumber": "101",
    "unitType": "Apartment",
    "floorNumber": 1,
    "areaSqm": 75.5,
    "isOccupied": false
  }'
```

### Crear un Residente

```bash
curl -X POST http://localhost:8080/api/residents \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant1" \
  -d '{
    "unitId": 1,
    "firstName": "Juan",
    "lastName": "Pérez",
    "documentNumber": "1234567890",
    "phone": "+57 300 9876543",
    "email": "juan.perez@example.com",
    "isOwner": true
  }'
```

## Estructura del Proyecto

```
src/
├── main/
│   ├── java/com/backendcr/residentialcomplex/
│   │   ├── config/          # Clases de configuración
│   │   ├── controller/      # Controladores REST
│   │   ├── dto/             # Objetos de Transferencia de Datos
│   │   ├── entity/          # Entidades JPA
│   │   ├── exception/       # Manejo de excepciones
│   │   ├── multitenancy/    # Infraestructura multitenant
│   │   ├── repository/      # Repositorios JPA
│   │   ├── service/         # Lógica de negocio
│   │   └── ResidentialComplexApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/backendcr/residentialcomplex/
        └── ResidentialComplexApplicationTests.java
```

## Arquitectura Multitenant

La aplicación utiliza una estrategia de **columna discriminadora** para multitenancy:

1. **Contexto de Inquilino**: Almacenamiento local de hilos para ID de inquilino actual
2. **Interceptor de Inquilino**: Extrae ID de inquilino de encabezados HTTP
3. **Entidades Conscientes de Inquilino**: Clase de entidad base que establece automáticamente ID de inquilino
4. **Filtros Hibernate**: Filtrado automático de consultas por ID de inquilino

## Pruebas

Ejecuta las pruebas con:

```bash
mvn test
```

## Licencia

Este proyecto es con fines educativos.