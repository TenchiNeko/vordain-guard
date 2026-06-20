# Vordain Guard v0.1 Local Beta Test Plan

## Installation

1. Run `tools/export_beta_apks.sh`.
2. Start a local APK download server:
   `cd ~/tablet-download && python3 -m http.server 8080 --bind 192.168.68.81`.
3. Install `vordain-guard-parent-0.1.0-beta.1.apk` on the parent test device.
4. Install `vordain-guard-child-0.1.0-beta.1.apk` on the child test tablet.
5. Start the local dev relay with `tools/run_dev_relay.sh`.

## Parent To Child

1. Open Vordain Guard Parent Beta.
2. Confirm parent and child device IDs.
3. Build a DNS policy.
4. Preview a domain decision.
5. Build the parent sync bundle.
6. Send through Local dev relay, Android share sheet, or copy/paste fallback.
7. On the child app, fetch or import the parent bundle.
8. Confirm the child reports the policy payload was verified before use.

## Basic DNS Guard

1. In the child app, request VPN permission.
2. Review hardening and bypass-risk checklist items.
3. Start Basic DNS Guard.
4. Confirm runtime state shows Running, descriptor established, and DNS loop running.
5. Test an allowed domain.
6. Test a blocked sample domain.
7. Test `dns.google` or `cloudflare-dns.com` and confirm encrypted-DNS seed blocking.
8. Stop Basic DNS Guard.
9. Confirm state changes to Stopped and heartbeat stops.

## Hardening

Review and confirm:

* Always-on VPN.
* Block connections without VPN.
* Settings/App Lock or screen pinning.
* Developer Options and ADB disabled.
* No unrestricted secondary users/profiles.
* Private DNS reviewed.

## Child To Parent

1. Build child sync bundle.
2. Send through Local dev relay, Android share sheet, or copy/paste fallback.
3. Parent fetches or imports child bundle.
4. Confirm parent displays child status, alerts, hardening, bypass risk, active policy, heartbeat, and sync state.

## Failure Testing

Test:

* Relay offline.
* Malformed bundle.
* Wrong-direction bundle.
* Invalid policy payload.
* VPN revoke.
* App restart while Basic DNS Guard was expected.
* Relay restart losing queued messages.

Expected result: apps show Needs attention, Stopped, or Unknown instead of claiming full protection.
