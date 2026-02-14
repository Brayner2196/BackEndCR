# BackEndCR - Residential Complex Multitenant Application

Backend application for managing residential complexes (conjuntos residenciales) with multitenant architecture using Spring Boot 3.2.2 and Java 21.

## Technology Stack

- **Java**: 21 (OpenJDK)
- **Spring Boot**: 3.2.2
- **Database**: H2 (in-memory)
- **ORM**: Hibernate/JPA
- **Build Tool**: Maven
- **Architecture**: Multitenant with discriminator column

## Features

- **Multitenant Architecture**: Single database with tenant isolation using discriminator column
- **RESTful API**: Complete CRUD operations for:
  - Residential Complexes
  - Residential Units
  - Residents
- **H2 Console**: Web interface for database management
- **Validation**: Input validation using Bean Validation
- **Exception Handling**: Global exception handler with proper error responses

## Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

## Building the Project

```bash
mvn clean install
```

## Running the Application

```bash
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## H2 Database Console

Access the H2 console at: `http://localhost:8080/h2-console`

- **JDBC URL**: `jdbc:h2:mem:residentialdb`
- **Username**: `sa`
- **Password**: (leave empty)

## Multitenant Usage

The application uses a header-based tenant identification. Include the `X-Tenant-ID` header in your requests:

```bash
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/complexes
```

If no tenant header is provided, the system uses a default tenant.

## API Endpoints

### Residential Complexes

- `GET /api/complexes` - Get all complexes
- `GET /api/complexes/{id}` - Get complex by ID
- `GET /api/complexes/search?name={name}` - Search complexes by name
- `POST /api/complexes` - Create new complex
- `PUT /api/complexes/{id}` - Update complex
- `DELETE /api/complexes/{id}` - Delete complex

### Residential Units

- `GET /api/units` - Get all units
- `GET /api/units/{id}` - Get unit by ID
- `GET /api/units/complex/{complexId}` - Get units by complex
- `POST /api/units` - Create new unit
- `PUT /api/units/{id}` - Update unit
- `DELETE /api/units/{id}` - Delete unit

### Residents

- `GET /api/residents` - Get all residents
- `GET /api/residents/{id}` - Get resident by ID
- `GET /api/residents/unit/{unitId}` - Get residents by unit
- `GET /api/residents/search?lastName={lastName}` - Search residents by last name
- `POST /api/residents` - Create new resident
- `PUT /api/residents/{id}` - Update resident
- `DELETE /api/residents/{id}` - Delete resident

## Example Requests

### Create a Residential Complex

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

### Create a Residential Unit

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

### Create a Resident

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

## Project Structure

```
src/
├── main/
│   ├── java/com/backendcr/residentialcomplex/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/             # Data Transfer Objects
│   │   ├── entity/          # JPA entities
│   │   ├── exception/       # Exception handling
│   │   ├── multitenancy/    # Multitenant infrastructure
│   │   ├── repository/      # JPA repositories
│   │   ├── service/         # Business logic
│   │   └── ResidentialComplexApplication.java
│   └── resources/
│       └── application.properties
└── test/
    └── java/com/backendcr/residentialcomplex/
        └── ResidentialComplexApplicationTests.java
```

## Multitenant Architecture

The application uses a **discriminator column strategy** for multitenancy:

1. **Tenant Context**: Thread-local storage for current tenant ID
2. **Tenant Interceptor**: Extracts tenant ID from HTTP headers
3. **Tenant-Aware Entities**: Base entity class that automatically sets tenant ID
4. **Hibernate Filters**: Automatic filtering of queries by tenant ID

## Testing

Run the tests with:

```bash
mvn test
```

## License

This project is for educational purposes.
