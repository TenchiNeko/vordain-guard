# Vordain Guard

Vordain Guard is an Android-first phone safety system from Vordain Labs LLC for high-risk family safety cases where a child is actively bypassing ordinary parental controls.

## Local MVP Status

The current Android build exposes a local/debug Basic DNS Guard MVP. Basic DNS Guard uses DNS-only enforcement, blocks normal DNS-based access for blocked domains, forwards allowed DNS through the lab upstream path, and does not inspect or forward non-DNS traffic. It is not full protection, filtering is not production-enabled yet, and backend/relay production sync is not enabled.

The first product promise for Vordain Guard Basic is:

```text
No silent bypass.
```

Vordain Basic is bypass-resistant, but it is not full protection. It is designed to make DNS Guard state visible, enforce local policy while Basic DNS Guard is active, and alert parents quickly if DNS Guard is stopped, stale, degraded, or no longer confirmed.

The local MVP now uses debug/local sync bundles for the parent-child loop. The parent app can bundle policy and pairing payloads into one parent-to-child text export, and the child app can bundle status, alerts, hardening, bypass-risk, active policy, audit, and diagnostics into one child-to-parent text export. Production sync is not enabled yet and will use encrypted relay later.

Android share-sheet import is supported for these local sync bundles. Copy/paste remains available as a fallback, and each app keeps a local bundle inbox summary so accepted, rejected, and wrong-direction imports are visible during tablet testing.

A local dev relay is also available for manual send/fetch on a trusted LAN. It uses an in-memory JVM server and Android `HttpURLConnection` clients to exchange the same debug sync bundles. It is local/debug only, has no production encryption or authentication, and production sync will use encrypted relay later.

The product loop is:

```text
Policy -> Enforcement -> Event -> Encrypted Alert
```

## Product Tiers

Vordain Basic is the first market-validation product:

- Android-first
- Basic DNS Guard local MVP
- Proxy/anonymizer blocking through DNS policy
- Local cached policy enforcement
- VPN stopped detection
- Heartbeat freshness status
- Local parent-visible alerts
- Clear status: Ready for DNS Guard, Needs attention, Stopped, Unknown

Vordain Managed is a future premium tier:

- Android Enterprise or MDM-style enrollment
- Force-installed app
- Always-on VPN lockdown
- App uninstall and settings restrictions
- Stronger anti-tamper controls

Detection and transparency come first. Managed enforcement comes later.

## Local MVP States

Ready for DNS Guard means setup is confirmed enough for local DNS-only testing:

- VPN permission confirmed
- Local policy loaded
- Heartbeat fresh when Basic DNS Guard is running
- Required hardening checks parent-confirmed

Needs attention means the setup is weakened:

- VPN active but setup incomplete
- Policy stale
- Heartbeat delayed
- Fail-closed setting not enabled
- Another weakening condition exists

Stopped means Basic DNS Guard is known to have stopped:

- VPN revoked
- App disabled
- Basic DNS Guard explicitly stopped
- Local tamper detected

Unknown means Basic DNS Guard is no longer confirmed:

- Heartbeat missing
- Child device stopped reporting
- Parent should not assume DNS Guard is active

## Alert Paths

Confirmed stopped alert:

- The child app detects VPN disabled or revoked while still alive.
- It records a local child alert and exports a parent-visible debug report by copy/share for now.

Dead-man heartbeat alert:

- The child device tracks a foreground-service heartbeat while Basic DNS Guard is running.
- If heartbeat becomes stale or missing, local status marks Basic DNS Guard stale, missing, Unknown, or Stopped.
- Parents can import the child alert/status report and see that DNS Guard is no longer confirmed.

If Basic DNS Guard stops, the local alert center records it. If the device stops reporting, parent-visible status should be treated as Unknown until the child report is refreshed.

## Current Architecture Status

Current tested pure Kotlin pipeline:

```text
DomainName normalization
-> policy engine
-> proxy/anonymizer classification
-> DNS parsing
-> DNS traffic evaluation
-> security event creation
-> heartbeat/protection state
-> local event queue
-> encrypted relay/outbox contracts
-> future Android VPN service adapter
-> future parent alerts
```

The VPN service enforces policy but does not decide policy. All allow/block decisions go through `core/policy`.

Compatibility Mode is local. Approved apps can learn and cache required service domains on device so apps such as school or streaming apps can keep working without broad manual allowlists. Browsers, search apps, and unknown web apps should default to Strict mode. Hard safety blocks always win, including explicit blocklists, proxy/anonymizer signals, private DNS or VPN infrastructure intelligence, and crisis lockdown.

Parent Review is parent-initiated and minimized. A parent may submit a specific domain or app issue for review, but this is not passive browsing telemetry.

Subscription entitlements are separate from child safety data. Paid access is modeled through local entitlement leases and feature flags, not through readable child activity.

## What v1 Is

Vordain Guard v1 includes the architecture for:

- Android child app skeleton
- Android parent app skeleton
- Local VPN service stub
- DNS/domain monitoring architecture
- Allowlist/blocklist policy engine
- Proxy/anonymizer classifier
- DNS query parsing and DNS traffic evaluation
- Security event creation
- Heartbeat/protection state evaluation
- Local event queue
- Encrypted relay and outbox contracts for future production sync
- App-aware Compatibility Mode contracts
- Parent-initiated review request contracts
- Subscription entitlement contracts
- Parent-child local alert architecture with future encrypted alert contracts
- VPN stopped/tamper event model
- Setup checklist architecture

## What v1 Is Not

Vordain Guard v1 does not include:

- iOS
- Managed-device enrollment
- Full traffic decryption
- AI monitoring
- Social media message scanning
- Location tracking
- Production backend
- Real subscription billing
- Screenshots
- Message content monitoring
- Claims of absolute bypass prevention

Vordain Basic cannot fully prevent uninstall, force-stop, VPN/settings tamper by a determined user, use of another device, friend's phone, school computer, game console browser, hidden second phone, offline content, or communication inside approved encrypted apps unless the app is blocked entirely.

## Privacy Summary

Readable child activity should not be stored on Vordain servers. Child activity alerts should be minimal and eventually encrypted between parent and child devices. The future backend should support account, billing, encrypted relay, heartbeat, and protection state.

Vordain Guard v1 has no ads, no data resale, no behavioral tracking, no screenshots, no message-content monitoring, and no secret monitoring.

## Build Notes

This repository uses Kotlin and Gradle Kotlin DSL. Android modules are scaffolded, but this repository intentionally does not include generated files, signing keys, API keys, or production credentials.

## Current Status

Scaffold and pure Kotlin foundation. This is not production-ready software.
