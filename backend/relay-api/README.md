# Local Dev Relay API

Local JVM relay for manual Vordain Guard debug sync bundles.

It stores messages in memory only and defaults to `127.0.0.1:8081`. The relay
has no authentication or TLS and must remain on loopback. Every checked-in
Android variant rejects cleartext traffic and ships without a relay URL, so the
apps do not connect to this HTTP service out of the box.

This is not production sync and does not provide production security.
