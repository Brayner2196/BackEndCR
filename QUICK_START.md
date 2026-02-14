# Quick Start Guide

## Prerequisites
- Java 21
- Maven 3.6+

## Run the Application

### 1. Clone and Build
```bash
git clone https://github.com/Brayner2196/BackEndCR.git
cd BackEndCR
mvn clean install
```

### 2. Run
```bash
mvn spring-boot:run
```

Application starts on: `http://localhost:8080`

### 3. Access H2 Console
Open browser: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:residentialdb`
- Username: `sa`
- Password: (empty)

## Quick Test

### Create a Complex (Tenant 1)
```bash
curl -X POST http://localhost:8080/api/complexes \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant1" \
  -d '{
    "name": "Conjunto Los Pinos",
    "address": "Calle 123, Bogotá",
    "phone": "+57 300 1234567",
    "email": "info@lospinos.com",
    "totalUnits": 50
  }'
```

### Get All Complexes (Tenant 1)
```bash
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/complexes
```

### Create a Complex (Tenant 2)
```bash
curl -X POST http://localhost:8080/api/complexes \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: tenant2" \
  -d '{
    "name": "Conjunto Las Palmas",
    "address": "Carrera 45, Medellín",
    "phone": "+57 300 9876543",
    "email": "info@laspalmas.com",
    "totalUnits": 30
  }'
```

### Verify Isolation
```bash
# Tenant 1 should only see "Los Pinos"
curl -H "X-Tenant-ID: tenant1" http://localhost:8080/api/complexes

# Tenant 2 should only see "Las Palmas"
curl -H "X-Tenant-ID: tenant2" http://localhost:8080/api/complexes
```

## API Documentation

All endpoints require the `X-Tenant-ID` header.

### Complexes
- `GET /api/complexes` - List all
- `POST /api/complexes` - Create
- `GET /api/complexes/{id}` - Get one
- `PUT /api/complexes/{id}` - Update
- `DELETE /api/complexes/{id}` - Delete

### Units
- `GET /api/units` - List all
- `POST /api/units` - Create
- `GET /api/units/{id}` - Get one
- `PUT /api/units/{id}` - Update
- `DELETE /api/units/{id}` - Delete
- `GET /api/units/complex/{complexId}` - Units by complex

### Residents
- `GET /api/residents` - List all
- `POST /api/residents` - Create
- `GET /api/residents/{id}` - Get one
- `PUT /api/residents/{id}` - Update
- `DELETE /api/residents/{id}` - Delete
- `GET /api/residents/unit/{unitId}` - Residents by unit

## Notes

- If no `X-Tenant-ID` header is provided, the system uses "default" tenant
- All data is isolated by tenant - each tenant only sees their own data
- H2 database is in-memory - data is lost when application stops
