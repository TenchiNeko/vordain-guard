# Privacy Model

## Zero-readable-data goal

Vordain Guard is designed around a zero-readable-data architecture for child activity. Readable child activity must never be sent to Vordain servers.

## Backend boundary

The backend, when added later, must not receive readable child activity. It should act as an encrypted relay and account/billing service only. Relay payloads should be opaque to Vordain.

## Parent-child encryption

Parent-child alerts should be encrypted for the parent device. Parent policy should be signed so the child device can verify that policy changes came from a trusted parent device.

## Local enforcement

The child device should enforce policy locally. The child device can maintain a local policy cache, local settings, and local event queue so enforcement does not depend on readable server-side analysis.

## Exclusions in v1

Vordain Guard v1 does not include screenshots, message-content monitoring, secret monitoring, ads, selling data, behavioral tracking, or covert surveillance behavior.

## Product posture

The app must be disclosed as parental-control or family safety software. It should not be positioned as hidden monitoring software.
