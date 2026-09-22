# Vordain Guard Remote Test Harness

This directory documents reference code for a debug/local tablet testing harness. It is not production sync, a remote administration feature, or a hidden controller.

The public Android configuration intentionally leaves the relay URL blank and rejects cleartext traffic in every build variant. The bundled apps therefore do not connect to the HTTP relay out of the box. Contributors who need end-to-end testing must supply their own HTTPS endpoint or keep any local-only configuration in an uncommitted fork. Production sync will use an encrypted relay later, and release builds include no active remote-test controller behavior.

## Safety Boundaries

- Remote test mode must be visibly enabled inside the parent or child app.
- Commands are allowlisted app-level actions only.
- The harness does not run shell commands.
- The harness does not tap arbitrary UI coordinates.
- The harness does not install or uninstall apps.
- The harness does not bypass Android VPN permission prompts.
- The harness does not inspect HTTPS content, packet contents, browsing history, app usage, screenshots, messages, credentials, passwords, or PIN input.
- The relay has no authentication or TLS. Its safe default is loopback-only.
- Use a specific LAN bind only on a trusted, isolated lab network. Never expose the relay to the internet.
- No checked-in Android manifest opts into cleartext traffic.

## Relay Endpoints

Existing sync-bundle endpoints remain unchanged:

- `GET /health`
- `POST /debug/v1/messages`
- `GET /debug/v1/messages?targetDeviceId=...`
- `POST /debug/v1/ack?messageId=...`
- `GET /debug/v1/stats`

Remote test endpoints:

- `POST /debug/v1/test-commands`
- `GET /debug/v1/test-commands?targetDeviceId=...`
- `POST /debug/v1/test-results`
- `GET /debug/v1/test-results?targetDeviceId=...`

The relay remains in-memory. Restarting the relay loses queued commands and results.

## Event Log

The relay writes metadata-only NDJSON events to:

```text
backend/relay-api/.dev-relay-events.ndjson
```

The log records request time, method, path, remote address, accepted/rejected result, message id, command id, result id, and source/target device ids when available. It does not log bundle bodies, browsing content, traffic content, packet bytes, credentials, passwords, or PIN input.

## Optional end-to-end setup

The checked-in configuration supports server-side relay testing only. To connect Android devices, first provide a trusted HTTPS front end or make your own uncommitted local-only networking configuration. That opt-in is deliberately not part of the public repository.

For server-side checks, start the relay on loopback:

```bash
./tools/run_dev_relay.sh
```

After supplying a secure endpoint, enter its `https://` URL in both debug apps, visibly enable remote test mode, and leave the apps visible while testing. The debug controller polls only while that mode is enabled.

## Server Commands

From the repo root:

```bash
tools/vordain_remote_test.sh health
tools/vordain_remote_test.sh stats
tools/vordain_remote_test.sh parent-health
tools/vordain_remote_test.sh child-health
tools/vordain_remote_test.sh parent-send-policy
tools/vordain_remote_test.sh child-fetch-policy
tools/vordain_remote_test.sh child-import-policy
tools/vordain_remote_test.sh child-send-status
tools/vordain_remote_test.sh parent-fetch-status
tools/vordain_remote_test.sh child-start-guard
tools/vordain_remote_test.sh child-stop-guard
tools/vordain_remote_test.sh summary
```

Optional environment variables:

```bash
VORDAIN_RELAY_URL=https://YOUR_TEST_RELAY
VORDAIN_PARENT_DEVICE_ID=parent-debug-device
VORDAIN_CHILD_DEVICE_ID=child-debug-device
```

## Supported Commands

Parent app:

- relay health check
- send latest parent policy bundle to child through local dev relay
- fetch child status bundles from local dev relay
- ack latest fetched relay message
- export/build parent debug snapshot bundle

Child app:

- relay health check
- fetch parent policy bundle from local dev relay
- import latest fetched parent policy bundle through the verified policy path
- send child status bundle to parent through local dev relay
- send heartbeat/status report through the existing child status bundle path
- start Basic DNS Guard through the normal Android VPN permission-safe path
- stop Basic DNS Guard

## Release Safety

The active controllers live only in Android `src/debug` source sets:

- `apps/parent-app/src/debug/.../ParentRemoteTestController.kt`
- `apps/child-app/src/debug/.../ChildRemoteTestController.kt`

Release source sets contain no-op implementations with the same API. Tests verify those release stubs do not include polling implementation markers.

Every checked-in Android manifest rejects cleartext traffic, the default relay URL is blank, and the verification script fails if a manifest enables cleartext. The HTTP relay and command script remain as auditable reference infrastructure; Android end-to-end use requires an explicit contributor-owned secure configuration.
