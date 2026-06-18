# Architecture

## Product Goal

Vordain Guard is a high-risk Android phone safety product for parents whose children are actively bypassing ordinary parental controls.

Vordain Guard Basic is not trying to provide managed-device lockdown without managed-device enrollment. The first product promise is "No silent bypass." Protected means actually protected. If protection stops, parents are told. If the device stops reporting, parents are told that protection is no longer confirmed.

The core loop is:

```text
Policy -> Enforcement -> Event -> Encrypted Alert
```

## Basic And Managed

Vordain Basic is the first Android-first product:

- VPN/domain filtering
- Proxy/anonymizer blocking
- Local cached policy enforcement
- VPN stopped detection
- Heartbeat/dead-man protection status
- Parent alerts
- Clear status: Protected, Degraded, Stopped, Unknown

Vordain Managed is a future premium evolution:

- Android Enterprise or MDM-style enrollment
- Force-installed app
- Always-on VPN lockdown
- App uninstall and settings restrictions
- Stronger anti-tamper controls

Detection and transparency come first; managed enforcement comes later.

## Android-First Scope

The initial platform is Android only. The child device enforces local protection through local policy, local DNS/domain evaluation, and a future VPN service adapter. The parent device controls policy, sees protection state, and reviews alerts.

## Why iOS Is Deferred

iOS has a more restrictive path for VPN, device policy, background service behavior, and family safety enforcement. Android gives the MVP a more practical path for local VPN filtering, protection-state visibility, and future managed-device exploration.

## Protection States

Protected:

- VPN active
- Local policy loaded
- Heartbeat fresh
- Protection is currently confirmed

Degraded:

- VPN active but setup incomplete
- Policy stale
- Heartbeat delayed
- Fail-closed setting not enabled
- Another weakening condition exists

Stopped:

- VPN revoked
- App disabled
- Protection explicitly stopped
- Local tamper detected

Unknown:

- Heartbeat missing
- Device stopped reporting
- Parent should not assume protection is active

## Alert Paths

Confirmed stopped alert:

- App detects VPN disabled or revoked while still alive and able to communicate.
- It sends a parent alert immediately when possible.

Dead-man heartbeat alert:

- Child device periodically proves protection is active.
- If check-ins stop, backend or parent-side state marks protection Unknown or Stopped.
- Parent receives an alert that protection is no longer confirmed.

## Tested Pipeline

The current pure Kotlin foundation supports this pipeline:

```text
DomainName normalization
-> policy engine
-> proxy/anonymizer classification
-> DNS parsing
-> DNS traffic evaluation
-> security event creation
-> future heartbeat/protection state
-> future Android VPN service adapter
-> future parent alerts
```

## Parent App Responsibilities

The parent app owns parent-facing workflows:

- Pairing with a child device
- Viewing protection status summaries
- Editing allowlists, blocklists, and lockdown modes
- Reviewing decrypted alerts
- Following setup checklist steps

The parent app must not make direct enforcement decisions. It creates policy changes and displays protection state.

## Child App Responsibilities

The child app owns child-device workflows:

- Protection status display
- VPN consent and setup entry points
- Local enforcement status
- Protection stopped and tamper detection signals
- Heartbeat/protection-state reporting
- Local encrypted event queue

The child app must keep enforcement local and must not send readable child activity to Vordain servers.

## Policy Engine Responsibilities

`core/policy` is the single source of truth for allow/block decisions. It evaluates domains, app package names, lockdown mode, unknown-domain strategy, and known proxy/anonymizer policy.

Policy logic is pure Kotlin and has no Android UI, storage, or VPN service dependency.

## VPN Engine Responsibilities

`vpn/engine` orchestrates classifier signals, DNS parse results, policy evaluation, and traffic decisions. It can depend on `core/policy`, but it must not own policy rules.

The Android `VpnService` is only a future platform adapter. Packet handling and Android service lifecycle belong outside the pure Kotlin decision path.

## Crypto/Event Responsibilities

`core/events` defines parent-visible security events. Alerts leaving the child device should eventually be encrypted for the parent device. A tamper-evident event chain can be added later without changing the product loop.

`core/crypto` defines interfaces for device keys, encryption, signing, and key storage. Real crypto is intentionally deferred until the key model is reviewed.

## Data/Relay Responsibilities

`data/local` owns local policy cache, settings, pairing data, audit history, and event queue interfaces.

`data/relay` defines a relay client interface only. A future backend should act as an encrypted relay, account, billing, heartbeat, and protection-state service. It should not receive readable child browsing history.

## Dependency Direction

Allowed direction:

- `apps -> features -> core`
- `apps -> data`
- `apps:child-app -> vpn:service`
- `vpn:service -> vpn:engine`
- `vpn:engine -> core:policy`
- `vpn:engine -> vpn:dns`
- `vpn:engine -> vpn:classifier`
- `vpn:dns -> core:model`
- `vpn:classifier -> core:model`
- `data:relay -> core:crypto`
- `data:local -> core:model`
- `features -> core:model`
- `features -> data interfaces`

## Forbidden Dependency Direction

Forbidden direction:

- `core -> apps`
- `core -> features`
- `core -> Android UI`
- `core:policy -> vpn:service`
- `core:policy -> data storage`
- `vpn:dns -> core:policy`
- `vpn:dns -> vpn:engine`
- `vpn:classifier -> core:policy`
- `vpn:engine -> parent dashboard`
- `vpn:engine -> Compose UI`
- `data -> Compose UI`
- `features -> raw packet parsing`
- `VpnService -> hardcoded blocklist logic`
- `Activity/ViewModel/Composable -> direct policy decision logic`

## MVP Boundaries

Vordain Guard v1 includes an Android parent app skeleton, Android child app skeleton, local VPN service stub, DNS/domain monitoring architecture, allowlist/blocklist policy engine, proxy/anonymizer classifier, DNS parser, DNS traffic evaluation, security event creation, parent-child encrypted alert architecture, VPN stopped/tamper event model, and setup checklist architecture.

Vordain Guard v1 does not include iOS, managed-device enrollment, full traffic decryption, AI monitoring, social media message scanning, location tracking, production backend, subscription billing, screenshots, message content monitoring, or any claim of absolute bypass prevention.
