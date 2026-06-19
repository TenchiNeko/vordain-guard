# Vordain Guard

Vordain Guard is an Android-first phone safety system from Vordain Labs LLC for high-risk family safety cases where a child is actively bypassing ordinary parental controls.

## Local MVP Status

The current Android build exposes a local/debug Basic DNS Guard MVP. Basic DNS Guard uses DNS-only enforcement, blocks normal DNS-based access for blocked domains, forwards allowed DNS through the lab upstream path, and does not inspect or forward non-DNS traffic. It is not full protection, filtering is not production-enabled yet, and backend/relay production sync is not enabled.

The first product promise for Vordain Guard Basic is:

```text
No silent bypass.
```

Vordain Basic is bypass-resistant, not unbypassable. It is designed to make protection state visible, enforce local policy while protection is active, and alert parents quickly if protection is disabled, degraded, or no longer confirmed.

The product loop is:

```text
Policy -> Enforcement -> Event -> Encrypted Alert
```

## Product Tiers

Vordain Basic is the first market-validation product:

- Android-first
- VPN/domain filtering
- Proxy/anonymizer blocking
- Local cached policy enforcement
- VPN stopped detection
- Heartbeat/dead-man protection status
- Parent alerts
- Clear status: Protected, Degraded, Stopped, Unknown

Vordain Managed is a future premium tier:

- Android Enterprise or MDM-style enrollment
- Force-installed app
- Always-on VPN lockdown
- App uninstall and settings restrictions
- Stronger anti-tamper controls

Detection and transparency come first. Managed enforcement comes later.

## Protection States

Protected means actually protected:

- VPN active
- Local policy loaded
- Heartbeat fresh
- Protection is currently confirmed

Degraded means protection is weakened:

- VPN active but setup incomplete
- Policy stale
- Heartbeat delayed
- Fail-closed setting not enabled
- Another weakening condition exists

Stopped means protection is known to have stopped:

- VPN revoked
- App disabled
- Protection explicitly stopped
- Local tamper detected

Unknown means protection is no longer confirmed:

- Heartbeat missing
- Child device stopped reporting
- Parent should not assume protection is active

## Alert Paths

Confirmed stopped alert:

- The child app detects VPN disabled or revoked while still alive.
- It sends a parent alert immediately when possible.

Dead-man heartbeat alert:

- The child device periodically proves protection is active.
- If check-ins stop, backend or parent-side state marks protection Unknown or Stopped.
- Parents are told that protection is no longer confirmed.

If protection stops, parents are told. If the device stops reporting, parents are told that protection is no longer confirmed.

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
- Encrypted relay and outbox contracts
- App-aware Compatibility Mode contracts
- Parent-initiated review request contracts
- Subscription entitlement contracts
- Parent-child encrypted alert architecture
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
