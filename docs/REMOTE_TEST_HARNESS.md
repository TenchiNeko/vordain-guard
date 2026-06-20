# Vordain Guard Remote Test Harness

This is a debug/local tablet testing harness for the local dev relay. It is not production sync, not a remote administration feature, and not a hidden controller.

Production sync will use encrypted relay later. Release app builds include no active remote test controller behavior.

## Safety Boundaries

- Remote test mode must be visibly enabled inside the parent or child app.
- Commands are allowlisted app-level actions only.
- The harness does not run shell commands.
- The harness does not tap arbitrary UI coordinates.
- The harness does not install or uninstall apps.
- The harness does not bypass Android VPN permission prompts.
- The harness does not inspect HTTPS content, packet contents, browsing history, app usage, screenshots, messages, credentials, passwords, or PIN input.
- Use only on a trusted local lab network.

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

## Tablet Setup

1. Start the local dev relay:

   ```bash
   ./tools/run_dev_relay.sh
   ```

2. Open the parent app and child app on the tablets.
3. Confirm both apps point at the local relay URL, for example `http://192.168.68.81:8081`.
4. In each app, open the Local dev relay section.
5. Tap **Enable remote test mode**.
6. Leave the app visible while testing. The debug controller polls every few seconds only while this mode is enabled.

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
VORDAIN_RELAY_URL=http://192.168.68.81:8081
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
