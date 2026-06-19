# Privacy Model

## Zero-Readable-Data Goal

Vordain Guard is designed around a zero-readable-data architecture for child activity. Vordain should not store readable child browsing history on company servers.

## Backend Boundary

The backend, when added later, should support:

- Account
- Billing
- Encrypted relay
- Heartbeat
- Protection state

The backend should not receive readable child browsing history. Relay payloads should be opaque to Vordain.

## Alerts And Encryption

Child activity alerts should be minimal and focused on protection/security events, such as a single blocked domain, VPN stopped state, or missing heartbeat. Alerts should eventually be encrypted parent-to-child and child-to-parent where applicable.

Parent policy should be signed so the child device can verify that policy changes came from a trusted parent device.

## Local Enforcement

The child device should enforce policy locally. The child device can maintain a local policy cache, local settings, protection state, and local event queue so enforcement does not depend on readable server-side analysis.

Compatibility Mode runs locally. Approved apps can learn/cache required service domains on device, but this must not become a passive readable browsing feed to Vordain.

Signed intelligence bundles are local metadata for compatibility and risk signals. They must not be built from passive child browsing history in v1.

Parent Review is parent-initiated and minimized. A parent may submit a specific domain or app issue, but full URLs, query strings, screenshots, messages, and traffic logs should not be submitted.

Entitlements and subscriptions are separate from child activity. Billing status should be represented by entitlement leases and feature flags, not by child traffic or alert contents.

## Local MVP Debug Exports

The local MVP build uses explicit copy/share actions for pairing, policy payloads, setup reports, bypass-risk reports, child status reports, diagnostics, and local audit summaries. These exports are user-triggered and local/debug only.

The local audit timeline records explicit app actions such as starting DNS-only lab, applying a policy payload, generating reports, and copying diagnostics. It must not become passive browsing, packet-content, screenshot, message, account-secret, PIN, or telemetry collection.

Production relay and backend sync are not enabled in the local MVP build.

## No Secret Monitoring

Vordain Basic should be disclosed parental-control and family safety software. It should not be positioned as hidden monitoring software and should not secretly monitor children.

## Exclusions In V1

Vordain Guard v1 does not include:

- Ads
- Data resale
- Behavioral tracking
- Screenshots
- Message-content monitoring
- Secret monitoring
- Covert surveillance behavior

## Product Posture

Vordain Basic is about detection and transparency first. If protection is active, parents can see that it is confirmed. If protection is degraded, stopped, or unknown, parents are told.
