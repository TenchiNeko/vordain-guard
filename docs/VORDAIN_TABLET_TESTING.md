# Vordain Guard Tablet Testing

This guide is for private debug APK testing.

## Export APKs

Run:

```bash
tools/export_debug_apks.sh
```

The script builds debug APKs, copies them to `~/tablet-download`, writes `SHA256SUMS`, and prints a local server command:

```bash
cd ~/tablet-download && python3 -m http.server 8080 --bind 192.168.68.81
```

## Install From Tablet

1. Connect the tablet to the same local network.
2. Open the local server URL from the tablet browser.
3. Download `vordain-guard-child-debug.apk`.
4. Install the APK after confirming the Android install prompt.

## DNS-Only Lab Test

1. Open the child app.
2. Request VPN permission.
3. Start DNS-only lab.
4. Open a browser.
5. Test an allowed site.
6. Test a blocked domain from the sample or debug policy.
7. Return to the child app and review DNS-only lab counters.
8. Stop DNS-only lab.

Expected behavior:

* Allowed DNS can be forwarded to the lab upstream DNS server.
* Blocked DNS receives a local synthetic block response.
* Non-DNS traffic is not inspected in DNS-only mode.
* This does not block DoH, direct-IP bypasses, cached DNS, or non-DNS paths yet.
* Filtering is not production-enabled yet.
* This is not full protection.

## DNS Bypass Hardening Review

Before treating a tablet as ready for DNS-only lab testing, review the child app's DNS bypass hardening panel:

* Turn off Android Private DNS or set it to a parent-approved provider.
* Remove or block alternate VPN apps.
* Remove or block proxy apps and private browsers.
* Confirm Developer Options, USB debugging, and wireless debugging are off.
* Confirm no unrestricted secondary users or profiles are available.
* Acknowledge that direct-IP and app-level encrypted DNS paths are not fully handled by DNS-only mode.

The child app can copy a debug bypass-risk report. Paste that report into the parent app to review the parent-visible DNS-only risk status.

## Encrypted DNS Resolver Test

The lab build includes a small, non-exhaustive encrypted-DNS resolver seed list for DNS-only bypass testing. Try resolving one of these sample domains while DNS-only lab mode is running:

* `dns.google`
* `cloudflare-dns.com`
* `dns.quad9.net`

Expected behavior:

* Matching resolver domains are blocked through DNS-only lab handling.
* Normal allowed DNS can still forward to the lab upstream DNS server.
* Non-DNS traffic is not inspected or forwarded by Vordain in DNS-only mode.

For stronger device hardening during tests, use Always-on VPN, Block without VPN, Settings/App Lock, Private DNS review, Developer Options/ADB off, no alternate VPN/proxy/private browsers, and no unrestricted profiles where the device supports those settings.
