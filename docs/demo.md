# A five-minute NetCore walkthrough

## Prepare

Start the local application and optionally run `node scripts/seed-demo.mjs`. The sample uses the same REST endpoints as the dashboard. It does not insert directly into the database.

## Demonstrate

1. **Overview:** show available and allocated addresses, utilization, subnet counts, and the device breakdown. These values come from inventory records, not network monitoring.
2. **Address space:** create `192.0.2.0/29` with gateway `192.0.2.1`. There are eight addresses: three reserved and five assignable.
3. **Devices:** register `demo-server`, then add `eth0` with MAC `02:00:00:00:10:01`.
4. **Assignment:** select the new subnet and interface. Leave Specific IP blank. NetCore assigns `192.0.2.2`, the lowest available address.
5. **Conflict:** try assigning `192.0.2.2` again. The API returns a conflict; it does not reassign the address silently.
6. **Release:** release that address, confirm, and show the available count return to five.
7. **Inspect:** filter allocated/reserved addresses, search a subnet, and export the summary to CSV.

## Explain the important decisions

**What is the project?** A bounded IP address inventory with devices and interfaces. Its main technical problem is avoiding duplicate assignments under concurrent requests.

**Why is a transaction alone insufficient?** Two transactions can both read the same available address before either updates it. Allocation first locks the subnet row, so the second request waits and makes its decision after the first transaction finishes.

**Why lock the subnet?** It gives next-available selection and release one shared coordination point. It is simple to reason about, at the cost of serializing operations within a subnet.

**Why database constraints as well?** The global unique address constraint rejects overlapping pools even if concurrent creates pass application checks. Foreign keys protect relationships. The allocation lock and unique address constraint solve different races.

**What was optimized?** Subnet summaries use one grouped count query and one subnet query. Interface inventory loads interfaces/devices and assignments in two queries. The browser fetches subnets, devices, and interfaces in parallel. This reduces request/query growth; it is not a claim of measured latency or production scale.

**What is deployed?** Say what you actually ran: H2 locally, Oracle Compose locally, or a specific hosted deployment. Deployment configuration and an Oracle integration test are not proof that a public service is running.

## Read the implementation in this order

| Question | Source |
| --- | --- |
| How are network and broadcast addresses calculated? | `backend/src/main/java/dev/wali/netcore/subnet/Ipv4AddressCalculator.java` |
| What happens when a subnet is created? | `subnet/SubnetPoolPersistenceService.java` |
| Where is the database lock acquired? | `allocation/IpAllocationService.java` and `subnet/SubnetRepository.java` |
| Where do dashboard counts come from? | `subnet/IpAddressRepository.java` and `subnet/SubnetManagementService.java` |
| How is the interface inventory assembled? | `device/DeviceManagementService.java` |
| How does a UI mutation refresh the inventory? | `frontend/src/App.tsx` |
| What proves contention behavior? | `backend/src/test/java/dev/wali/netcore/allocation/IpAllocationServiceTests.java` and `integration/OraclePersistenceIT.java` |

Paths abbreviated after the first Java row are relative to `backend/src/main/java/dev/wali/netcore/`; the Oracle test path is relative to `backend/src/test/java/dev/wali/netcore/`.

Be comfortable explaining a /29 example and tracing one allocation end to end. Describe tools or assistance used honestly when asked; use the implementation and checks as evidence for behavior.
