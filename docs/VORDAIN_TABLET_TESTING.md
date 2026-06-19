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

For stronger device hardening during tests, use Always-on VPN, Block without VPN, and Settings/App Lock where the device supports those settings.
