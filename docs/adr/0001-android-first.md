# ADR 0001: Android First

## Decision

Android is the first platform because Android VPN, service, and device-policy paths are more practical for the MVP than iOS.

## Context

Vordain Guard v1 needs local enforcement on the child device, local VPN architecture, protection stopped detection, and a setup checklist for stronger device configuration. Android offers a more direct MVP path for these requirements.

## Consequences

The initial repository prioritizes Android package structure, Android app modules, and Android VPN service boundaries. iOS is deferred until the Android product loop is validated.
