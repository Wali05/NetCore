# Verification record

## Release checks — 23 October 2026

Run locally with Docker Desktop before pushing this revision.

| Check | Result |
| --- | --- |
| Maven `clean verify` with Java 25 | **62 tests passed**, zero failures/errors |
| Oracle integration profile (`-P oracle-integration verify`) | `OraclePersistenceIT` **passed** against `gvenzl/oracle-free:slim-faststart` |
| Frontend `npm ci`, formatting, TypeScript, production build | Passed; `npm audit` reports 0 vulnerabilities after patch updates to Vite 6.4.5 and source-map-js 1.2.2 |
| Container CI job, run locally under a separate Compose project | Stack became healthy; seeding through Nginx gave 4 subnets and 23 allocations; 8 interfaces remained after a backend restart |
| CSV export | Cells are quoted when they contain commas, quotes or line breaks; text starting with `=`, `+`, `-` or `@` is prefixed so spreadsheets do not run it as a formula |

The hosted HTTPS/password flow is still not verified or deployed.

## Interface refinement — 21 October 2026

The additional interface pass passed frontend formatting, TypeScript, and production build checks. Browser checks against the running Spring Boot/H2 API confirmed:

- The capacity review prompt opens the correct subnet; gateway, broadcast, and availability match the stored pool.
- Sorting by utilization and searching/clearing the subnet list update the displayed rows and result count.
- Selecting a free address tile fills the specific-IP field and moves focus to the interface selector. Keyboard Enter activates the tile as well.
- Assigning `10.20.30.6` to `monitoring-01 / eth0` updates the map, table, and pool summary to full. Releasing the temporary assignment restores the original inventory.
- Pagination shows only that page's addresses; selecting Reserved from page two resets to page one with the three reserved records.
- Overview, address space, and devices were checked at 320 pixels; after removing the fixed body minimum, document scroll width matched client width (305 pixels with the desktop scrollbar). Tables retain their own horizontal scrolling.
- Desktop layout was inspected at 1440 pixels. Device details show actual interface and assignment counts.

No backend code or dependencies changed in this additional pass, so the earlier backend test result below remains the relevant recorded run; it was not rerun for CSS/components. These are targeted UI checks, not a full accessibility certification. See [the research and design review](design-review.md).

## Initial redesign — 19 October 2026

Checked locally on **19 October 2026**. This record describes evidence from the working tree, not a release or hosted service.

| Check | Result |
| --- | --- |
| Maven `verify` with Java 25 | **62 tests passed**, zero failures/errors |
| Inventory query-count tests on H2 | Two SQL statements for subnet listing; two for the all-interface inventory, including multiple devices and assignments |
| Frontend formatting, TypeScript, production build | Passed |
| Sample-data script | Created 4 subnets, 8 devices, 8 interfaces, 23 allocations; a second run left the inventory unchanged |
| Browser → API → H2 workflow | Created a subnet, device, and interface; allocated and released an address; observed updated counts |
| Duplicate allocation | UI displayed the API's conflict message; inventory was not reassigned |
| Search, pagination, filtering | Subnet/device search, page changes, and reserved-address filtering checked in the browser |
| CSV export | Downloaded CSV inspected; headers and rows matched the loaded subnet inventory |
| Connection loss and recovery | Last snapshot marked stale and writes disabled; initial failure showed a connection screen; retry recovered |
| Responsive layout | Desktop at 1440 px and mobile at 390 px inspected; no document overflow after fixing the hidden table-label positioning |
| Base and deployment Compose configuration | Both passed `config --quiet` with a standalone Compose CLI |
| Nginx configuration | Syntax check passed using local upstream/static-path substitutions |
| Caddy configuration | Validation passed with a temporary test hash and example hostname; no certificate issued or public listener started |
| New Oracle assertions and container CI job | Added; **not executed locally** because no Docker engine was available |
| Hosted HTTPS/password flow | **Not verified or deployed** |

The local frontend build used Node 20.17.0; Node 22 remains the documented and CI runtime. No frontend dependencies were added.

## What the new tests establish

`InventoryReadTests` checks empty inventory, numeric subnet ordering, available/allocated/reserved counts, interface assignment grouping, release updates, and SQL statement counts. This is a focused regression check for the batched reads, not a load test.

The existing allocation tests continue to cover contention and invalid state transitions. The Oracle integration test now includes the new aggregate and interface queries after its migration/concurrency workflow. It must be run on Docker before claiming fresh Oracle verification for this revision.

## Remaining release checks

On a Docker-capable host, run the Oracle integration profile and the full Compose stack. The new container CI job builds the images, seeds through Nginx, and checks inventory after restarting the backend. Its presence in the workflow is not evidence that the remote job has passed.

Before sharing a hosted demo, verify the actual domain, certificate, unauthorized UI/API responses, and record persistence using the [deployment guide](deployment.md). No GitHub push or public deployment was performed during this work.
