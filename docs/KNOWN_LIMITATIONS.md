# Known Limitations

Vordain Guard v0.1 local beta is not full protection.

Current limits:

* Basic DNS Guard is DNS-only enforcement.
* Non-DNS traffic is not inspected.
* General TCP forwarding is not implemented.
* General UDP forwarding is not implemented except the protected upstream DNS transport.
* Direct-IP traffic may bypass DNS policy.
* DoH endpoint seed blocking is non-exhaustive.
* Cached DNS and app-level encrypted DNS can still require Android hardening.
* Local dev relay is in-memory reference infrastructure, disabled end to end in the stock Android configuration, and not production secure.
* No production encryption or authentication is implemented.
* No automatic background cloud sync exists.
* No MDM or Android Enterprise management is implemented.
* Debug APK signing only.
* Full-tunnel lab remains a developer tool and may break internet while active.
