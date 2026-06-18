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
