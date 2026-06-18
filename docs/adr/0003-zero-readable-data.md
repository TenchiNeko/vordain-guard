# ADR 0003: Zero Readable Data

## Decision

Vordain servers should not store readable child activity. Alerts leaving the child device should be encrypted for the parent.

## Context

The product handles sensitive family safety signals. Server-side readability would create avoidable privacy and security risk.

## Consequences

Crypto and relay boundaries are part of the initial scaffold. Relay interfaces must document that payloads are encrypted before leaving the device. Any future backend work must preserve this boundary.
