# Threat Model

## Scope

This v1 threat model focuses on bypass-resistant Android enforcement for high-risk family safety cases. The system is designed to make policy enforcement local, visible, and auditable without sending readable child activity to Vordain servers.

## Threats in scope for v1

- Child attempts proxy/anonymizer domains
- Child attempts VPN workaround
- Child disables protection
- Child uses unknown domains during lockdown mode
- Child uses alternate browser or app path where detectable
- Child uses cellular instead of home Wi-Fi
- Parent is non-technical and needs plain-English alerts

## Threats not fully solved in v1

- Friend's phone
- School device
- Game console browser
- Hidden second device
- Offline content
- Approved encrypted apps unless blocked entirely
- Factory reset without managed-device enrollment
- Physical coercion or stolen parent passcode

## Design response

The child device enforces locally through a VPN-based architecture and local policy cache. The policy engine decides whether observed domains or app network attempts should be allowed, blocked, or alert-only. The event layer records parent-visible security events, and alerts are intended to be encrypted for the parent before relay.

The product should be described as bypass-resistant, not unbypassable.
