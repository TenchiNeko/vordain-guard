# Vordain Guard

Vordain Guard is an Android-first phone safety system from Vordain Labs LLC for high-risk family safety cases where a child is actively bypassing ordinary parental controls.

The product loop is:

```text
Policy -> Enforcement -> Event -> Encrypted Alert
```

The child device enforces protection locally. The parent device controls policy and receives encrypted alerts. A future backend must act only as a blind encrypted relay plus billing/account service.

## What v1 is

Vordain Guard v1 includes the architecture for:

- Android child app skeleton
- Android parent app skeleton
- Local VPN service stub
- DNS/domain monitoring architecture
- Allowlist/blocklist policy engine
- Proxy/anonymizer classifier stub
- Parent-child encrypted alert architecture
- VPN stopped/tamper event model
- Setup checklist architecture

## What v1 is not

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

The intended language is bypass-resistant, managed-device lockdown, fail-closed protection, and zero-readable-data architecture.

## Architecture summary

The VPN service enforces policy but does not decide policy. All allow/block decisions go through `core/policy`.

Allowed dependency direction:

- `apps -> features -> core`
- `apps -> data`
- `apps:child-app -> vpn:service`
- `vpn:service -> vpn:engine`
- `vpn:engine -> core:policy`
- `vpn:classifier -> core:model`
- `data:relay -> core:crypto`
- `data:local -> core:model`

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

## Privacy summary

Readable child activity must never be sent to Vordain servers. Alerts and logs leaving the child device must be encrypted for the parent device. The backend, when added later, is a blind encrypted relay.

Vordain Guard v1 has no ads, no selling data, no behavioral tracking, no screenshots, no message-content monitoring, and no secret monitoring.

## Build notes

This repository uses Kotlin and Gradle Kotlin DSL. Android modules are scaffolded, but this repository intentionally does not include generated files, binary files, signing keys, API keys, or production credentials.

This scaffold does not install dependencies and does not require Android Studio to inspect the code.

## Current status

Scaffold only. This is not production-ready software.
