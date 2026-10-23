# How NetCore works

NetCore is one application with a React interface, a Spring Boot REST API, and a relational database. The dashboard calls the same API available to other clients. The backend keeps code for each workflow together in `subnet`, `device`, and `allocation` packages; request and response records keep HTTP data separate from JPA entities.

```text
Browser → React dashboard → Spring Boot API → Oracle Free
                                      └──────→ H2 for local backend development
```

The active backend profile selects one database; the API does not write to both.

The Docker frontend uses Nginx as a same-origin proxy. An optional deployment overlay puts a Caddy HTTPS/password gateway in front of it. The Java container runs as an unprivileged user; the database and API have no published ports. See [deployment](deployment.md) for the boundary and setup.

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

## Inventory reads

The overview loads subnets, devices, and all interfaces in three parallel HTTP requests. The previous flow fetched interfaces once per device after the device list completed; the collection endpoint removes that dependency.

Subnet listing uses two queries: a grouped address count by subnet/status and a subnet list. Counts for absent statuses become zero. The small subnet list is sorted numerically in Java so `10.0.2.0` precedes `10.0.10.0`; it is not sorted as ordinary text.

Interface listing also uses two queries: interfaces with their devices, then assigned addresses with their subnets. The service groups assignments by interface ID and returns response records. Address-page queries fetch their to-one interface/device relationships together to avoid loading those relationships once per displayed row.

`InventoryReadTests` checks returned counts, ordering, empty inventory, release behavior, and SQL statement counts on H2. No caching layer is needed at this scale. Inventory lists remain unpaginated, so memory and result size still grow with inventory size. Separate HTTP reads form a recent snapshot, not an atomic cross-resource snapshot.

## Frontend behavior

React state coordinates the selected subnet/device, address filters, and forms. A successful mutation refreshes the inventory; address pages are requested only on the address-space screen. Effect cleanup aborts obsolete reads, and requests have a 15-second deadline. A read failure offers a retry instead of leaving a permanent loading label. A lost inventory connection retains the last snapshot, labels it stale, and disables writes until a successful refresh.

The capacity ring and bars are derived from stored counts. Reserved addresses are excluded from utilization. The UI does not invent historical trends, uptime, topology links, or network-discovery results.

## Boundary

NetCore tracks address assignments; it does not configure devices or DHCP servers. The application has no individual accounts, roles, or audit trail. The default stack is local-only; the optional gateway is a shared password gate for private demonstrations. IPv6 and larger pools need a different address representation than one row per address.
