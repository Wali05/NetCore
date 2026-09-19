# NetCore

NetCore is a small IP address management system for modelling networks, materializing IPv4 address pools, and assigning addresses to network interfaces.

The project is being built incrementally around explicit domain rules and database-backed workflows. The current backend exposes the main IPAM workflow through a REST API: create a subnet pool, register a device and its interfaces, assign an address, and release it.

The scope is intentionally small. NetCore is currently an IPAM backend, not a complete network automation platform. DHCP integration and an operations dashboard remain later work.

## Current capabilities

- Model devices, network interfaces, subnets, and IPv4 addresses
- Convert IPv4 addresses between textual and numeric forms
- Calculate network and broadcast addresses
- Materialize address pools for `/24` through `/30` subnets
- Reserve network, broadcast, and configured gateway addresses
- Persist a subnet and its generated pool in one transaction
- Query addresses in numeric order, with pagination and status filtering
- Allocate a requested address or the next available address
- Release an allocated address
- Create devices and attach uniquely named network interfaces
- Inspect the addresses assigned to an interface
- Validate API requests and return structured errors for invalid or conflicting operations
- Preserve assignments, statuses, and timestamps across database reloads
- Reject invalid, reserved, duplicate, missing, and exhausted allocation requests

Automated tests cover the domain rules, pool generation, persistence, database constraints, allocation, release, transaction rollback, and complete HTTP workflows.

## How it fits together

A `Subnet` defines an IPv4 network. Creating one also creates its complete address pool. Network, broadcast, and configured gateway addresses are marked as reserved; the remaining addresses begin as available.

A `Device` owns one or more `NetworkInterface` records. Allocation links an available `IpAddress` to an interface and records when the assignment happened. Releasing it removes that link and returns the address to the available pool.

Controllers deal with HTTP requests, application services coordinate each transaction, domain objects enforce local rules, and repositories handle persistence. The API uses request and response records instead of serializing JPA entities directly.

The backend is organized by feature:

```text
dev/wali/netcore/
├── allocation/  # assign and release addresses
├── device/      # devices and network interfaces
├── health/      # application health endpoint
├── shared/      # common API responses and error handling
└── subnet/      # subnet models, address pools, and persistence
```

Each feature keeps its controller, service, persistence, and API types together. Tests follow the same layout, with cross-feature HTTP workflows under `integration/`.

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

The application starts at `http://localhost:8080`.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/v1/subnets` | Create a subnet and materialize its address pool |
| `GET` | `/api/v1/subnets` | List subnets and their utilization |
| `GET` | `/api/v1/subnets/{id}` | Get one subnet |
| `GET` | `/api/v1/subnets/{id}/addresses` | Page through addresses, optionally filtered by status |
| `POST` | `/api/v1/subnets/{id}/addresses/{address}/allocate` | Assign a requested address to an interface |
| `POST` | `/api/v1/subnets/{id}/allocate-next` | Assign the lowest available address to an interface |
| `POST` | `/api/v1/subnets/{id}/addresses/{address}/release` | Release an assigned address |
| `POST` | `/api/v1/devices` | Create a device |
| `GET` | `/api/v1/devices` | List devices |
| `POST` | `/api/v1/devices/{id}/interfaces` | Add an interface to a device |
| `GET` | `/api/v1/devices/{id}/interfaces` | List a device's interfaces and assignments |
| `GET` | `/api/v1/interfaces/{id}` | Get one interface and its assignments |
| `GET` | `/api/v1/health` | Check application health |
| `GET` | `/actuator/health` | Check Spring Boot health details |

Local development uses an in-memory H2 database.

### Typical workflow

1. Create a subnet with `POST /api/v1/subnets`.
2. Create a device with `POST /api/v1/devices`.
3. Add an interface with `POST /api/v1/devices/{deviceId}/interfaces`.
4. Assign a requested address or use `POST /api/v1/subnets/{subnetId}/allocate-next`.
5. Inspect the assignment through `GET /api/v1/interfaces/{interfaceId}`.
6. Release the address when it is no longer needed.

Create requests use JSON. For example:

```json
{
  "networkAddress": "10.20.30.0",
  "prefixLength": 29,
  "gatewayAddress": "10.20.30.1"
}
```

## Run the tests

From the `backend` directory:

```powershell
.\mvnw.cmd test
```

Use `./mvnw test` on Linux or macOS.

## Oracle compatibility check

With a working Docker engine, run this from `backend`:

```powershell
.\mvnw.cmd -P oracle-integration verify
```

On Linux or macOS, use `./mvnw -P oracle-integration verify`. The optional integration test starts an Oracle Free container, creates a temporary schema, then checks pool persistence, concurrent allocation, and release on Oracle. It does not require an Oracle Cloud account. The regular test command does not start a container.

This integration check has passed against Oracle Free. The `oracle` application profile expects an existing schema and validates it at startup. A migration or schema provisioning step is still needed before using that profile for a persistent deployment.

## Current limitations

- Allocation and release serialize on the subnet row. Concurrent allocation is covered on H2 and Oracle Free.
- The Oracle integration test needs a working Docker engine. It verifies the core persistence and allocation workflow, not a persistent Oracle deployment.
- The operations dashboard, Docker environment, and CI workflow have not been added.

## Next steps

- Add a focused React operations dashboard
- Add Docker-based local execution and automated CI checks
- Add repeatable schema provisioning for persistent Oracle deployment

DHCP integration and reconciliation remain possible future extensions after the smaller IPAM application is complete.
