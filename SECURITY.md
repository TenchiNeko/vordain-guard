# Security Policy

## Support status

Vordain Guard is unfinished, experimental, unaudited software. There are no supported production releases and no security-service-level agreement.

| Version | Supported |
| --- | --- |
| Current `main` branch | Best-effort review only |
| APKs or forks from this repository | Not supported |
| Production deployments | Not supported or recommended |

## Reporting a vulnerability

Do not publish exploit details, credentials, real child data, or sensitive family information in a public issue.

1. Use [GitHub private vulnerability reporting](https://github.com/TenchiNeko/vordain-guard/security/advisories/new) when that option is available.
2. If it is unavailable, open a minimal issue titled **Security contact request** without technical exploit details or sensitive data. A private channel can then be arranged.

Include the affected commit, component, build variant, reproduction conditions, impact, and a minimal synthetic proof of concept.

## Known design limitations

The following are documented limitations of the prototype, not undisclosed vulnerabilities by themselves:

- the local development relay has no authentication or TLS;
- the relay is intended only for loopback;
- Basic DNS Guard is DNS-only and does not forward or inspect general non-DNS traffic;
- the project has no production backend, cloud sync, managed-device enforcement, or release signing;
- no checked-in Android build variant permits cleartext traffic, so the HTTP relay is not connected to the bundled apps out of the box;
- release remote-test controllers are deliberate no-op implementations.

Reports are still valuable when code crosses one of those boundaries—for example, active remote-test behavior in a release build, cleartext permission in any manifest, sensitive payload logging, arbitrary command execution, or a policy path that bypasses `core/policy`.

## Safe testing

Test only on devices and networks you own or are explicitly authorized to use. Use synthetic data. Do not expose the relay to the internet or test against children, families, schools, or third parties without explicit authorization.
