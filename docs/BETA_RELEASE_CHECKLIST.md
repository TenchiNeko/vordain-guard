# Vordain Guard v0.1 Beta Release Checklist

Before distributing debug APKs:

* Worktree is clean.
* Required pure tests pass.
* Relay tests pass.
* Parent app, child app, and VPN service assemble.
* Parent app, child app, and VPN service lint pass.
* `tools/verify_beta_candidate.sh` passes.
* `tools/export_beta_apks.sh` produces APKs and `SHA256SUMS`.
* `git ls-files local.properties` prints nothing.
* `git ls-files "*.apk"` prints nothing.
* Root `LICENSE` is the Apache License 2.0.
* Known limitations are reviewed with testers.
* Every checked-in Android manifest disallows cleartext traffic.
* Local dev relay uses the loopback default and is not exposed to the internet.

Beta APKs:

* `~/tablet-download/vordain-guard-parent-0.1.0-beta.1.apk`
* `~/tablet-download/vordain-guard-child-0.1.0-beta.1.apk`
