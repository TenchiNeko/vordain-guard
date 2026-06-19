# Vordain Guard Tablet Testing

This guide is for private debug APK testing.

## Export APKs

Run:

```bash
tools/export_debug_apks.sh
```

The script builds debug APKs, copies them to `~/tablet-download`, writes `SHA256SUMS`, and prints a local server command:

```bash
cd ~/tablet-download && python3 -m http.server 8080 --bind 192.168.68.81
```

If APKs are already built, you can skip the Gradle build:

```bash
tools/export_debug_apks.sh --skip-build
```

## Install From Tablet

1. Connect the tablet to the same local network.
2. Open the local server URL from the tablet browser.
3. Download `vordain-guard-child-debug.apk`.
4. Install the APK after confirming the Android install prompt.

## Basic DNS Guard Local MVP Test

1. Open the child app.
2. Request VPN permission.
3. Apply a debug policy from the parent app, or use the default sample policy.
4. Review the hardening and bypass-risk checklist.
5. Start Basic DNS Guard.
6. Open a browser.
7. Test an allowed site.
8. Test a blocked domain from the sample or debug policy.
9. Return to the child app and review Basic DNS Guard counters, readiness, active policy, and audit timeline.
10. Review heartbeat status and local alerts.
11. Stop Basic DNS Guard.

Expected behavior:

* Basic DNS Guard uses DNS-only enforcement and does not install default routes.
* Allowed DNS can be forwarded to the lab upstream DNS server.
* Blocked DNS receives a local synthetic block response.
* Encrypted-DNS resolver seed domains, such as `dns.google`, are blocked through DNS handling.
* Non-DNS traffic is not inspected or forwarded by Basic DNS Guard.
* This does not block every DoH, direct-IP, cached DNS, or app-level encrypted DNS path without additional hardening.
* Filtering is not production-enabled yet.
* This is not full protection.

## Local Alerts And Heartbeat

The child app includes a local "No silent bypass" alert center for explicit app/security events.

To test it:

1. Start Basic DNS Guard.
2. Confirm the child dashboard shows heartbeat fresh while the service is running.
3. Stop Basic DNS Guard and confirm a local stopped alert is added.
4. Use the debug stale-heartbeat simulation button to verify a critical alert appears.
5. Copy or share the child alert report.
6. Paste the report into the parent app's Child alerts section.
7. Confirm the parent view shows active critical alerts, latest severity, and policy version when present.

Alerts are local/debug copy-paste reports for now. Production alerts will use encrypted relay later. Vordain does not record PINs or account secrets, and alert reports do not include DNS observations, packet bytes, web history, or message contents.

## DNS-Only Lab Test

DNS-only lab remains available as a developer tool below Basic DNS Guard.

1. Open the child app.
2. Request VPN permission.
3. Apply a debug policy from the parent app, or use the default sample policy.
4. Start DNS-only lab.
5. Open a browser.
6. Test an allowed site.
7. Test a blocked domain from the sample or debug policy.
8. Return to the child app and review DNS-only lab counters.
9. Stop DNS-only lab.

Expected behavior:

* Allowed DNS can be forwarded to the lab upstream DNS server.
* Blocked DNS receives a local synthetic block response.
* Non-DNS traffic is not inspected in DNS-only mode.
* This does not block DoH, direct-IP bypasses, cached DNS, or non-DNS paths yet.
* Filtering is not production-enabled yet.
* This is not full protection.

## Local MVP Flow

Recommended parent app order:

1. Create pairing invite.
2. Build DNS policy.
3. Review hardening report.
4. Review child status report.
5. Adjust policy.

Recommended child app order:

1. Pair/debug handoff.
2. Request VPN permission.
3. Complete hardening setup.
4. Apply parent DNS policy.
5. Start Basic DNS Guard.
6. Generate child status report.
7. Parent reviews report.

The local MVP uses copy/paste and explicit share buttons. Production sync is not enabled yet.

## Parent DNS Policy Editor

Use the parent app's DNS policy editor to build a practical lab policy:

1. Open the parent app.
2. Enter the target child device id, such as `child-debug-device`.
3. Choose a preset:
   * Basic DNS Guard
   * Strict Browser
   * School Friendly
   * High Risk Lockdown
   * Custom
4. Edit allowed and blocked domains if needed.
5. Review proxy, encrypted-DNS resolver, and unknown-domain settings.
6. Use the policy preview field to test a user-entered domain.
7. Build the debug policy update.
8. Copy the debug policy update payload.

The editor is local/debug only. Production sync will use encrypted relay later.

## Child Active Policy Flow

Apply and verify the parent-built policy in the child app:

1. Open the child app.
2. Paste the debug policy update payload into the Debug policy handoff section.
3. Tap Apply debug policy update.
4. Confirm the active policy source says verified debug policy.
5. Check the active policy version, preset, allow/block counts, proxy setting, encrypted-DNS resolver setting, and unknown-domain behavior.
6. Use Re-verify stored policy after app restart to confirm the signed debug payload still verifies.
7. Use the domain tester to evaluate user-entered domains against the active policy.
8. Start Basic DNS Guard; DNS decisions use the verified active policy when available.

If the stored policy payload fails verification, the child app falls back to the default sample policy and reports the rejection reason. Summary fields are display-only; the signed debug payload remains the authority.

## Reports, Sharing, and Local History

The child app can copy or share local debug setup, bypass-risk, child status, diagnostics, active policy diagnostics, and audit summaries. The parent app can import child reports and keeps a bounded local report history for generated policy payloads, pairing payloads, setup reports, bypass-risk reports, and child status reports.

The child app can also copy/share a local alert report. The parent app can import child alert reports and keeps them in the same bounded local report history.

The local audit timeline records explicit app actions such as starting DNS-only lab, applying a policy payload, generating reports, and copying diagnostics. It is not a passive monitor.

QR-ready text envelopes are included around share payloads so they can later be encoded as QR content without adding a scanner dependency in this milestone.

## Policy Test Cases

With Basic DNS Guard running:

* Test an allowed domain from the active policy and confirm DNS forwarding counters move.
* Test a blocked sample domain, such as `blocked.example`, and confirm blocked response counters move.
* Test an encrypted-DNS resolver domain, such as `dns.google`, and confirm it is blocked through DNS handling.
* Test a non-DNS/direct-IP path only as a known limitation. DNS-only mode does not inspect non-DNS traffic.

## DNS Bypass Hardening Review

Before treating a tablet as ready for DNS-only lab testing, review the child app's DNS bypass hardening panel:

* Turn off Android Private DNS or set it to a parent-approved provider.
* Remove or block alternate VPN apps.
* Remove or block proxy apps and private browsers.
* Confirm Developer Options, USB debugging, and wireless debugging are off.
* Confirm no unrestricted secondary users or profiles are available.
* Acknowledge that direct-IP and app-level encrypted DNS paths are not fully handled by DNS-only mode.

The child app can copy a debug bypass-risk report. Paste that report into the parent app to review the parent-visible DNS-only risk status.

## Encrypted DNS Resolver Test

The lab build includes a small, non-exhaustive encrypted-DNS resolver seed list for DNS-only bypass testing. Try resolving one of these sample domains while DNS-only lab mode is running:

* `dns.google`
* `cloudflare-dns.com`
* `dns.quad9.net`

Expected behavior:

* Matching resolver domains are blocked through DNS-only lab handling.
* Normal allowed DNS can still forward to the lab upstream DNS server.
* Non-DNS traffic is not inspected or forwarded by Vordain in DNS-only mode.

For stronger device hardening during tests, use Always-on VPN, Block without VPN, Settings/App Lock, Private DNS review, Developer Options/ADB off, no alternate VPN/proxy/private browsers, and no unrestricted profiles where the device supports those settings.

## Full-Tunnel Lab Test

Full-tunnel lab remains a separate explicit mode. It may interrupt normal internet access because packets are routed into Vordain lab handling. Use it only for device smoke testing, then stop it from the child app.

Basic DNS Guard is the primary local MVP path for tablet testing because non-DNS traffic is not routed through Vordain in that mode.

## Local MVP Limitations

This build is not full protection:

* No general TCP forwarding.
* No non-DNS packet forwarding.
* No backend or relay sync.
* No production sync.
* No MDM controls.
* DNS-only mode does not inspect non-DNS traffic.
* DNS-only mode does not fully handle DoH, direct-IP access, cached DNS, or app-level encrypted DNS without additional hardening.

## Third-Party Notices

No root `LICENSE` file is added by these local MVP steps. `THIRD_PARTY_NOTICES.md` is a non-exhaustive placeholder for development tooling and dependency notices, including Gradle wrapper/build tooling under Apache 2.0.
