# Contributing to Vordain Guard

Thanks for helping improve Vordain Guard. This repository is an experimental reference project, not a supported production product. Keep changes narrow, testable, and honest about the current protection boundary.

## Development setup

You need JDK 17, Android SDK 35, Bash, and an Android API 26+ device or emulator.

Run the complete repository check before opening a pull request:

~~~bash
./tools/verify_beta_candidate.sh
~~~

For an app-only debug build:

~~~bash
./gradlew :apps:parent-app:assembleDebug :apps:child-app:assembleDebug
~~~

## Contribution workflow

1. Create a focused branch from `main`.
2. Keep unrelated cleanup out of the same change.
3. Add or update tests for behavior changes.
4. Update documentation when behavior, safety boundaries, or limitations change.
5. Run the complete verification script.
6. Open a pull request using the repository template.

## Non-negotiable boundaries

Contributions must preserve these rules:

- Never include real child activity, family information, credentials, keys, or production data in source, fixtures, logs, screenshots, or issues.
- Do not add covert monitoring, message capture, screenshot capture, credential capture, hidden control, or arbitrary remote execution.
- Keep readable child activity off future server-side paths.
- Keep policy decisions in `core/policy`, not in Android UI, storage, or `VpnService` code.
- Treat the local relay as unauthenticated debug infrastructure. It must default to loopback and must not become a production transport.
- Active remote-test behavior must remain debug-only; release controllers must remain no-op.
- No checked-in Android build variant may permit cleartext traffic.
- Describe the software as bypass-resistant research, never as unbypassable or production-ready.

Read [docs/CODEX_RULES.md](docs/CODEX_RULES.md), [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), and [docs/THREAT_MODEL.md](docs/THREAT_MODEL.md) before changing architectural boundaries.

## Style and tests

- Follow the existing Kotlin style and module boundaries.
- Prefer pure Kotlin logic with unit tests over logic embedded in Android components.
- Keep fixtures synthetic and obviously fictional.
- Use clear commit messages that explain the behavior changed.
- Do not commit APKs, signing material, `local.properties`, environment files, or generated build output.

## License

By submitting a contribution, you agree that it may be licensed under the repository's [Apache License 2.0](LICENSE).
