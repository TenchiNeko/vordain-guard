# Play Store Compliance

Vordain Guard must be clearly disclosed as parental-control and family safety software.

## Basic Positioning

Vordain Basic uses VPN functionality for parental-control and device-security purposes:

- Local VPN/domain filtering
- Proxy/anonymizer blocking
- Local cached policy enforcement
- VPN stopped detection
- Heartbeat/protection state
- Parent-visible alerts

VPN use must be disclosed clearly during setup and in store-facing materials.

## Consent And Visibility

Parent/guardian consent must be explicit. The child-device setup flow must explain that local VPN protection is used for bypass-resistant policy enforcement.

The child device should show protection status:

- Protected
- Degraded
- Stopped
- Unknown

## Claims

Avoid hidden surveillance language and avoid claims of absolute bypass prevention. Use accurate wording:

- No silent bypass
- Bypass-resistant, not unbypassable
- Protected means actually protected
- If protection stops, parents are told
- If the device stops reporting, parents are told that protection is no longer confirmed

## Local MVP Disclosure Draft

Current local MVP/debug builds should be described as DNS-only lab filtering and setup/report testing. Store-facing or tester-facing copy should say:

- Filtering is not production-enabled yet.
- DNS-only lab filtering can block normal DNS-based access for configured domains.
- DNS-only lab filtering does not inspect non-DNS traffic.
- DoH, direct-IP access, cached DNS, alternate VPN/proxy apps, and private browsers require hardening review.
- Parent-visible reports are local copy/paste or explicit share exports until production sync exists.
- No backend, relay delivery, billing, MDM, or production sync is enabled in the local MVP build.

## Safety Boundaries

Vordain Guard must not include hidden spyware behavior, secret monitoring, screenshots, message-content monitoring, deceptive traffic routing, or covert traffic collection.

## Data Minimization

The app must not collect unnecessary readable child activity. Alerts and logs leaving the child device should be minimal and eventually encrypted for the parent device.

The backend should be used for account, billing, encrypted relay, heartbeat, and protection state. It should not store readable child browsing history.

Compatibility Mode, local intelligence, and local policy evaluation should be disclosed as part of parental-control/device-security functionality. They must not be positioned as hidden surveillance.

Parent Review should be described as a parent-initiated support/review workflow for a specific site or app problem, not passive telemetry.

Subscription entitlement checks must remain separate from child activity and must not require readable child browsing data.
