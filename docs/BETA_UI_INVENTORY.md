# Vordain Guard Beta UI Inventory

This inventory reflects the v0.1 local beta candidate surface before final tablet testing.

## Parent App Sections

The parent app is a single scrollable activity using the existing programmatic Android view stack.

Current primary sections:

1. Recommended local MVP order and checklist.
2. Child DNS Guard status.
3. Pairing invite and child acceptance.
4. DNS policy editor and policy preview.
5. Parent sync bundle export.
6. Local dev relay.
7. Child hardening setup report.
8. Child security status report.
9. Child alerts.
10. Child sync bundle import.
11. Bundle inbox.
12. DNS-only bypass-risk report.
13. Local report history.

## Child App Sections

The child app is also a single scrollable activity with grouped sections.

Current primary sections:

1. Local MVP flow and checklist.
2. Basic DNS Guard.
3. Setup and hardening controls.
4. Active policy and policy verification.
5. Pairing and debug policy handoff.
6. Local alerts.
7. Child sync bundle export.
8. Local dev relay.
9. Parent sync bundle import.
10. Bundle inbox.
11. Local audit timeline.
12. DNS-only lab tools.
13. Full-tunnel lab and developer diagnostics.

## Duplicated Controls

Some actions remain intentionally available in multiple paths:

* Sync bundle copy/share remains available beside local dev relay because relay is debug-only and can be offline.
* Status, alert, setup, and bypass reports remain importable individually for troubleshooting, while sync bundles are the preferred beta flow.
* DNS-only lab and full-tunnel lab tools remain below the primary Basic DNS Guard path for developer testing.

No duplicate heading was found that safely hid a second implementation of the same operation. The beta polish keeps existing working features accessible and groups the relay path as the preferred sync path.

## Current Primary Flow

Parent:

1. Identify parent and child device IDs.
2. Build and preview DNS policy.
3. Build parent sync bundle.
4. Send by local dev relay or Android share sheet.
5. Fetch/import child sync bundle.
6. Review child status, alerts, hardening, bypass risk, policy summary, and heartbeat.

Child:

1. Confirm child device identity.
2. Fetch/import parent bundle.
3. Verify and apply policy payload.
4. Request VPN permission.
5. Complete hardening and bypass-risk review.
6. Start Basic DNS Guard.
7. Export/send child sync bundle.

## Developer And Lab Tools

Developer tools remain visible but should be treated as secondary:

* DNS-only lab filtering.
* Full-tunnel lab packet capture.
* Raw diagnostic text.
* Simulated heartbeat/alert actions.

## Persistence Stores

Parent app:

* `ParentDebugStateStore`
* `ParentReportHistoryStore`
* `ParentBundleInboxStore`

Child app:

* `ChildDebugStateStore`
* `ChildAuditStateStore`
* `ChildAlertStateStore`
* `ChildBundleInboxStore`

## Sync Paths

Current sync paths:

* Parent-to-child Android share-sheet sync bundle.
* Child-to-parent Android share-sheet sync bundle.
* Copy/paste fallback for both directions.
* Manual local dev relay send/fetch/ack for both directions.

The relay is in-memory, local/debug only, and not production secure.

## Known UX Gaps For Tablet Testing

* Single long-scroll activities are functional but dense.
* Individual report import sections are still present for troubleshooting.
* Local dev relay requires manual IP/port configuration.
* Runtime status is local and conservative; stale status is shown as Unknown/Needs attention.
* Basic DNS Guard remains DNS-only and not full protection.
