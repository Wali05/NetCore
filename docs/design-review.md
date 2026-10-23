# Interface review and research

Reviewed on **21 October 2026** against the local implementation and primary sources. The purpose is to make the bounded IPAM workflow easier to inspect and demonstrate, without expanding the architecture unnecessarily.

## What informed the changes

| Reference | Relevant guidance | NetCore decision |
| --- | --- | --- |
| [NetBox IPAM documentation](https://netbox.readthedocs.io/en/stable/features/ipam/) | Prefixes, individual addresses, and utilization are core inventory concepts. NetBox also models larger hierarchies and VRFs. | Make the selected subnet's gateway, broadcast, reservations, and available capacity explicit. Keep NetCore's single address space and /24–/30 scope. |
| [IBM Carbon data tables](https://carbondesignsystem.com/components/data-table/usage/) | Tables benefit from clear toolbars, search, sorting, and consistent interaction sizes. | Add subnet sorting by network, highest utilization, and fewest available addresses; include clear-search and result counts. |
| [W3C target-size guidance](https://www.w3.org/WAI/WCAG22/Understanding/target-size-minimum.html) | Pointer targets should meet the 24 CSS pixel minimum or its spacing exceptions; larger controls help. | Increase copy buttons to 28 pixels, key actions to 44 pixels, and address tiles to at least 43 × 48 pixels. |
| [W3C reflow guidance](https://www.w3.org/WAI/WCAG22/Understanding/reflow.html) | Ordinary content should reflow at 320 CSS pixels; data tables may have their own horizontal scrolling. | Remove the 360-pixel page minimum and test 320-pixel layouts. Keep wide tables within their own scrolling region. |

These sources inform the design; they do not certify accessibility or establish feature parity with NetBox.

## Gaps found and addressed

- Secondary text and controls were too small for comfortable use. Increase typography, spacing, and click targets while retaining the existing visual identity.
- The overview showed capacity but had no direct prompt to inspect constrained pools. Add a notice when utilization reaches 80%, linking to the most utilized subnet. **80% is a UI heuristic**, not a forecast, outage alert, or industry standard.
- Pool selection lacked context. Add a subnet detail summary and a segmented meter based on stored counts.
- The address list required scanning every row. Add a labeled map of the **current page and filter**, with text as well as color. Choosing a free tile fills the assignment form and focuses the interface selector; only submitting the form writes to the API.
- The assignment and address list were stacked in one desktop column. Put inventory/assignment on the left and the address pool on the right.
- Reserved addresses had an unexplained dash. Label network, gateway, and broadcast reservations.
- Validation hints were visual only. Associate the hints with their fields and expose invalid state to assistive technology.

## Scope and remaining gaps

The API still enforces allocation eligibility and concurrency. A tile reflects a fetched snapshot, so another request can allocate it before submission; API conflicts must remain visible. The map adds no new requests and never treats missing pages as free addresses.

NetCore has no device/subnet editing or deletion, individual accounts, role permissions, audit history, IPv6, VRFs, automatic discovery, or DHCP/DNS provisioning. These are separate product/backend changes, not interface polish. No fictitious activity feed, uptime chart, or performance figures were added.

Fresh Oracle integration tests, container runtime checks, and hosted HTTPS/password checks still require the environments described in [verification](verification.md) and [deployment](deployment.md). This review does not close those release checks. Accessibility checks are targeted; no full WCAG conformance claim is made.
