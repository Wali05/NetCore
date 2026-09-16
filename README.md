# NetCore

NetCore is a small IP address management system for modelling networks, materializing IPv4 address pools, and assigning addresses to network interfaces.

The project is being built incrementally around explicit domain rules and database-backed workflows. The current backend can generate and persist subnet pools, allocate addresses to interfaces, and release them back into the available pool.

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
- Preserve assignments, statuses, and timestamps across database reloads
- Reject invalid, reserved, duplicate, missing, and exhausted allocation requests

Automated tests cover the domain rules, pool generation, persistence, database constraints, allocation, release, and transaction rollback.

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

The application starts at `http://localhost:8080`. The endpoints currently exposed are:

- `GET /api/v1/health`
- `GET /actuator/health`

Local development uses an in-memory H2 database.

## Run the tests

From the `backend` directory:

```powershell
.\mvnw.cmd test
```

Use `./mvnw test` on Linux or macOS.

## Current limitations

- IPAM operations currently exist in the service layer and are not exposed through REST endpoints yet.
- Next-address allocation is correct for sequential requests but does not yet lock rows against concurrent allocators.
- Persistence tests currently run against H2. The Oracle profile is prepared but has not been verified against a real Oracle database.
- The operations dashboard, Docker environment, and CI workflow have not been added.

## Next steps

- Expose subnet, address, device, interface, allocation, and release workflows through REST
- Reproduce and solve concurrent next-address allocation
- Add a focused React operations dashboard
- Add Docker-based local execution and automated CI checks
- Verify database behavior against Oracle

DHCP integration and reconciliation remain possible future extensions after the smaller IPAM application is complete.
