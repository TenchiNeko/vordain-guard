# Codex Rules for Vordain Guard

## Product rule

Vordain Guard is a high-risk Android phone safety app. The child app enforces protection locally. The parent app controls policy and receives encrypted alerts.

## Architecture rule

Do not place business logic in Activity, Composable, ViewModel, or VpnService classes.

## Layer rule

UI may depend on use cases.

Use cases may depend on repositories and core models.

VPN service may depend on VPN engine.

VPN engine may depend on policy engine.

Policy engine must not depend on Android UI or storage.

## Privacy rule

Readable child activity must never be sent to the backend. Alerts and logs leaving the child device must be encrypted for the parent device.

## VPN rule

VpnService is only a platform adapter. It starts/stops the tunnel and forwards traffic to vpn-engine. It must not contain blocklist logic.

## Policy rule

All allow/block decisions must go through core/policy.

## Event rule

All parent-visible security events must be represented as core/events/SecurityEvent.

## Storage rule

Settings use data/local.

Policy cache uses data/local.

Audit/event history uses data/local.

No feature screen writes directly to storage.

## Testing rule

Every policy decision must have unit tests.

Every classifier must have fixture tests.

Every security event type must have serialization tests.
