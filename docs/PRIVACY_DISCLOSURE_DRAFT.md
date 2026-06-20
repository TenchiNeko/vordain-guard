# Privacy Disclosure Draft

Vordain Guard v0.1 local beta uses Android VPN APIs on the child device to support DNS-only enforcement in Basic DNS Guard.

The child device processes DNS-domain-level requests locally so blocked domains can receive a local DNS block response and allowed DNS can be forwarded upstream. Vordain Guard does not read HTTPS content, decrypt traffic, inspect messages, capture screenshots, or collect PIN input.

The beta does not store browsing history, full URLs, raw packet contents, app usage logs, screenshots, message content, credentials, passwords, PINs, or passive telemetry.

The local dev relay is a manual debug tool for parent/child sync bundles. It is in-memory, cleartext HTTP on a trusted local network, and not production secure. Production sync is not enabled in this beta and is planned to use encrypted relay later.

Parents explicitly share or fetch local beta reports. Readable child activity is not intended for company servers.

This beta is for local testing and is not full protection.
