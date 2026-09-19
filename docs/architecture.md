# How NetCore works

NetCore is one application with a React interface, a Spring Boot REST API, and a relational database. The dashboard calls the same API available to other clients. The backend keeps code for each workflow together in `subnet`, `device`, and `allocation` packages; request and response records keep HTTP data separate from JPA entities.

```text
Browser → React dashboard → Spring Boot API → Oracle Free
                                      └──────→ H2 for local backend development
```

The active backend profile selects one database; the API does not write to both.

## Data model

A `Subnet` owns its `IpAddress` rows. A `Device` owns `NetworkInterface` rows. An allocated address points to the interface using it and records when the assignment was made. Releasing it clears both the link and the allocation time.

Creating a subnet materializes every address in its `/24`–`/30` pool. The network and broadcast addresses are reserved; a configured gateway is reserved too. The subnet and pool are saved in one transaction, so a failed pool insert cannot leave behind an empty subnet.

NetCore has no routing-domain or VRF model. It therefore treats an IPv4 address as globally unique across its pools: an overlapping subnet fails at the database constraint, including when concurrent requests race to create one.

## Allocation and release

Both operations first take a pessimistic write lock on the subnet row. Allocation then checks the requested address or selects the lowest available address, changes its state, and links it to an interface. Release changes an allocated address back to available and removes that link. Invalid transitions return a conflict response.

The lock makes the decision and update one serial operation *per subnet*. Two callers cannot both read and claim the same available address. Calls concerning different subnets can proceed independently. This is a deliberate throughput trade-off: a busy subnet becomes a bottleneck, but the limited pool size keeps the mechanism simple to reason about and test.

## Schema and verification

The local profile uses in-memory H2 with Hibernate schema updates for fast development. The Oracle profile uses Flyway before Hibernate validates the schema:

1. `V1__create_ipam_tables.sql` creates the tables, relationships, and lookup index.
2. `V2__prevent_overlapping_address_pools.sql` adds a unique constraint on the address value.

Keeping V2 separate lets an Oracle database already at V1 upgrade without recreating its tables or losing assignments. Existing overlapping data would have to be resolved before that constraint could be applied.

The H2 tests cover domain rules, rollback, API behavior, and concurrent allocation. The Oracle integration test starts a fresh Oracle Free container, applies both migrations, validates the mapping, and exercises allocation under contention. The Docker stack also uses Oracle Free with a persistent named volume.

## Boundary

NetCore tracks address assignments; it does not configure devices or DHCP servers. Its dashboard has no authentication and is intended for local use. IPv6 and larger pools need a different address representation than one row per address.
