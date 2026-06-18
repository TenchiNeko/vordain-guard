# Architecture

## Product goal

Vordain Guard is a high-risk Android phone safety system for parents whose children are actively bypassing ordinary parental controls. The first milestone is not a complete product. It is a clean, modular scaffold that keeps policy, enforcement, storage, crypto, and UI responsibilities separate.

The core loop is:

```text
Policy -> Enforcement -> Event -> Encrypted Alert
```

## Android-first scope

The initial platform is Android only. The child device is expected to enforce local protection, including a local VPN adapter and local policy cache. The parent device is expected to edit policy and review decrypted alerts.

## Why iOS is deferred

iOS has a more restrictive path for VPN, device policy, background service behavior, and family safety enforcement. Android gives the MVP a more practical path for local VPN filtering, managed-device lockdown exploration, and fail-closed protection patterns. iOS can be revisited after the Android architecture proves the product loop.

## Parent app responsibilities

The parent app owns parent-facing workflows:

- Pairing with a child device
- Viewing protection status summaries
- Editing allowlists, blocklists, and lockdown modes
- Reviewing decrypted alerts
- Following setup checklist steps

The parent app must not make direct enforcement decisions. It creates signed policy changes that the child device can verify and enforce locally.

## Child app responsibilities

The child app owns child-device workflows:

- Protection status display
- VPN consent and setup entry points
- Local enforcement status
- Protection stopped and tamper detection signals
- Local encrypted event queue

The child app must keep enforcement local and must not send readable child activity to Vordain servers.

## Policy engine responsibilities

`core/policy` is the single source of truth for allow/block decisions. It evaluates domains, app package names, lockdown mode, unknown-domain strategy, and known proxy/anonymizer policy.

Policy logic is pure Kotlin and has no Android UI, storage, or VPN service dependency.

## VPN engine responsibilities

`vpn/engine` converts observed traffic metadata into policy evaluations and traffic decisions. It can depend on `core/policy`, but it must not own policy rules.

The Android `VpnService` is only a platform adapter. Actual packet handling, DNS/domain observation, and traffic decision forwarding belong below `vpn/engine` and related VPN modules.

## Crypto/event responsibilities

`core/crypto` defines interfaces for device keys, encryption, signing, and key storage. Real crypto is intentionally deferred until the key model is reviewed.

`core/events` defines parent-visible security events. Alerts leaving the child device must be encrypted for the parent device. A tamper-evident event chain can be added later without changing the product loop.

## Data/relay responsibilities

`data/local` owns local policy cache, settings, pairing data, audit history, and event queue interfaces.

`data/relay` defines a relay client interface only. A future backend must act as a blind encrypted relay and account/billing system. Relay payloads must already be encrypted before leaving the device.

## Dependency direction

Allowed direction:

- `apps -> features -> core`
- `apps -> data`
- `apps:child-app -> vpn:service`
- `vpn:service -> vpn:engine`
- `vpn:engine -> core:policy`
- `vpn:classifier -> core:model`
- `data:relay -> core:crypto`
- `data:local -> core:model`
- `features -> core:model`
- `features -> data interfaces`

## Forbidden dependency direction

Forbidden direction:

- `core -> apps`
- `core -> features`
- `core -> Android UI`
- `core:policy -> vpn:service`
- `core:policy -> data storage`
- `vpn:engine -> parent dashboard`
- `vpn:engine -> Compose UI`
- `data -> Compose UI`
- `features -> raw packet parsing`
- `VpnService -> hardcoded blocklist logic`
- `Activity/ViewModel/Composable -> direct policy decision logic`

## MVP boundaries

Vordain Guard v1 includes an Android parent app skeleton, Android child app skeleton, local VPN service stub, DNS/domain monitoring architecture, allowlist/blocklist policy engine, proxy/anonymizer classifier stub, parent-child encrypted alert architecture, VPN stopped/tamper event model, and setup checklist architecture.

Vordain Guard v1 does not include iOS, managed-device enrollment, full traffic decryption, AI monitoring, social media message scanning, location tracking, production backend, subscription billing, screenshots, message content monitoring, or any claim of absolute bypass prevention.
