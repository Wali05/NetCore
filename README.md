# NetCore

NetCore is a network source-of-truth and IP provisioning system. The goal is to explore how network inventory, IP allocation, and DHCP provisioning can be managed safely from one control plane.

The project is at an early stage. Right now, the repository contains the backend foundation: a Spring Boot application, local database configuration, an Oracle-ready profile, health endpoints, and a small test suite.

## Requirements

- Java 25

Maven is included through the Maven Wrapper, so a separate Maven installation is not required.

## Run locally

From the repository root on Windows:

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

On Linux or macOS:

```bash
cd backend
./mvnw spring-boot:run
```

The API starts at `http://localhost:8080`. Useful endpoints are:

- `GET /api/v1/health`
- `GET /actuator/health`

Local development uses an in-memory H2 database.

## Run the tests

From the `backend` directory:

```powershell
.\mvnw.cmd test
```

Use `./mvnw test` on Linux or macOS.

## Planned work

- Model devices, network interfaces, subnets, and addresses
- Allocate addresses safely under concurrent requests
- Persist production data in Oracle
- Integrate DHCP provisioning through Kea
- Recover and reconcile failed provisioning operations
- Add an operations dashboard
