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
3. Apply a debug policy from the parent app, or use the default sample policy.
4. Start DNS-only lab.
5. Open a browser.
6. Test an allowed site.
7. Test a blocked domain from the sample or debug policy.
8. Return to the child app and review DNS-only lab counters.
9. Stop DNS-only lab.

Expected behavior:

* Allowed DNS can be forwarded to the lab upstream DNS server.
* Blocked DNS receives a local synthetic block response.
* Non-DNS traffic is not inspected in DNS-only mode.
* This does not block DoH, direct-IP bypasses, cached DNS, or non-DNS paths yet.
* Filtering is not production-enabled yet.
* This is not full protection.

## Parent DNS Policy Editor

Use the parent app's DNS policy editor to build a practical lab policy:

1. Open the parent app.
2. Enter the target child device id, such as `child-debug-device`.
3. Choose a preset:
   * Basic DNS Guard
   * Strict Browser
   * School Friendly
   * High Risk Lockdown
   * Custom
4. Edit allowed and blocked domains if needed.
5. Review proxy, encrypted-DNS resolver, and unknown-domain settings.
6. Use the policy preview field to test a user-entered domain.
7. Build the debug policy update.
8. Copy the debug policy update payload.

The editor is local/debug only. Production sync will use encrypted relay later.

## Child Active Policy Flow

Apply and verify the parent-built policy in the child app:

1. Open the child app.
2. Paste the debug policy update payload into the Debug policy handoff section.
3. Tap Apply debug policy update.
4. Confirm the active policy source says verified debug policy.
5. Check the active policy version, preset, allow/block counts, proxy setting, encrypted-DNS resolver setting, and unknown-domain behavior.
6. Use Re-verify stored policy after app restart to confirm the signed debug payload still verifies.
7. Use the domain tester to evaluate user-entered domains against the active policy.
8. Start DNS-only lab; DNS decisions use the verified active policy when available.

If the stored policy payload fails verification, the child app falls back to the default sample policy and reports the rejection reason. Summary fields are display-only; the signed debug payload remains the authority.

## Policy Test Cases

With DNS-only lab running:

* Test an allowed domain from the active policy and confirm DNS forwarding counters move.
* Test a blocked sample domain, such as `blocked.example`, and confirm blocked response counters move.
* Test an encrypted-DNS resolver domain, such as `dns.google`, and confirm it is blocked through DNS-only lab handling.
* Test a non-DNS/direct-IP path only as a known limitation. DNS-only mode does not inspect non-DNS traffic.

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
