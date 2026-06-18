# Product Vision

## Core Thesis

Vordain Guard is a high-risk Android phone safety product for parents whose children are actively bypassing normal parental controls.

Vordain Guard Basic is not trying to provide managed-device lockdown without managed-device enrollment. The first product promise is:

```text
No silent bypass.
```

Basic should make protection state visible and alert parents quickly if VPN protection is disabled, degraded, or no longer confirmed.

## Product Promise

- Protected means actually protected.
- If protection stops, parents are told.
- If the device stops reporting, parents are told that protection is no longer confirmed.
- Bypass-resistant, not unbypassable.
- Detection and transparency first; managed enforcement later.

## Vordain Basic

Vordain Basic is the first market-validation product:

- Android-first
- VPN/domain filtering
- Proxy/anonymizer blocking
- Local cached policy enforcement
- VPN stopped detection
- Heartbeat/dead-man protection status
- Parent alerts
- Clear status: Protected, Degraded, Stopped, Unknown

## Vordain Managed

Vordain Managed is a future premium tier:

- Android Enterprise or MDM-style enrollment
- Force-installed app
- Always-on VPN lockdown
- App uninstall and settings restrictions
- Stronger anti-tamper controls

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

## Two Alert Paths

Confirmed stopped alert:

- App detects VPN disabled or revoked while still alive and able to communicate.
- It sends a parent alert immediately when possible.

Dead-man heartbeat alert:

- Child device periodically proves protection is active.
- If check-ins stop, backend or parent-side state marks protection Unknown or Stopped.
- Parent receives an alert that protection is no longer confirmed.

## Honest Limitations

Vordain Basic cannot fully prevent:

- Uninstall or force-stop on unmanaged devices
- VPN/settings tamper by a determined user
- Use of another device
- Friend's phone
- School computer
- Game console browser
- Hidden second phone
- Offline content
- Communication inside approved encrypted apps unless the app is blocked entirely

## Privacy Posture

Vordain should not store readable child browsing history on company servers. Child activity alerts should be minimal and eventually encrypted. The backend should support account, billing, encrypted relay, heartbeat, and protection state.

Vordain Basic has no ads, no data resale, no secret monitoring, and no screenshot/message-content monitoring in v1.

## Compatibility Mode

Compatibility Mode runs locally. Approved apps may learn and cache required service domains so legitimate school, education, or media apps can work without parents approving every hidden dependency by hand.

Browsers, search apps, and unknown web apps should default to Strict mode, not Compatibility Mode. Compatibility Mode is for known approved apps where the parent intends the app to work.

Hard safety blocks always win. Explicit blocklists, proxy/anonymizer signals, private DNS or VPN infrastructure intelligence, and crisis lockdown must not be bypassed by Compatibility Mode.

## Review, Intelligence, And Entitlements

Parent Review is parent-initiated and minimized. A parent may submit a specific website/domain/app issue to Vordain Review, but Vordain Basic should not send passive browsing history, full URLs, screenshots, messages, or traffic logs.

Intelligence bundles are signed local metadata for app compatibility profiles, proxy/anonymizer intelligence, and future rule updates. They must not be built from passive child browsing history in v1.

Entitlements and subscriptions are separate from child activity. Paid access should be represented by signed entitlement leases and local feature checks, not by readable child safety data.
