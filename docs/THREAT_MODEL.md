# Threat Model

## Scope

This v1 threat model focuses on bypass-resistant Android protection for high-risk family safety cases. Vordain Basic prioritizes detection and transparency: no silent bypass.

The product should be described as bypass-resistant, not unbypassable.

## Product Promise

Vordain Basic is designed so parents do not silently assume protection is active when it is not.

- Protected means actually protected.
- If protection stops, parents are told.
- If the device stops reporting, parents are told that protection is no longer confirmed.
- Detection and transparency first; managed enforcement later.

## Threats In Scope For Basic

- Child attempts proxy/anonymizer domains
- Child attempts VPN workaround
- Child disables or revokes VPN protection
- Child uses unknown domains during lockdown mode
- Child uses alternate browser or app path where detectable
- Child uses cellular instead of home Wi-Fi
- Protection heartbeat becomes stale or missing
- Parent is non-technical and needs plain-English alerts

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

- Child device must periodically prove protection is active.
- If check-ins stop, backend or parent-side state marks protection Unknown or Stopped.
- Parent receives an alert that protection is no longer confirmed.

This prevents silent failure even when the child app cannot send a final alert.

## Threats Not Fully Solved In Basic

- Uninstall or force-stop on unmanaged devices
- VPN/settings tamper by a determined user
- Friend's phone
- School device or school computer
- Game console browser
- Hidden second device or hidden second phone
- Offline content
- Approved encrypted apps unless blocked entirely
- Factory reset without managed-device enrollment
- Physical coercion or stolen parent passcode

## Managed Tier Response

Vordain Managed is a future premium tier that can explore Android Enterprise or MDM-style enrollment, force-installed apps, always-on VPN lockdown, app uninstall/settings restrictions, and stronger anti-tamper controls.

Managed-device enforcement is not required for first market validation, but it is the natural path for stronger protection after Basic proves value.
