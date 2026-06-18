# ADR 0004: Policy Engine Boundaries

## Decision

All allow/block decisions go through `core/policy`. VPN service and UI cannot make policy decisions directly.

## Context

The VPN service enforces policy but does not decide policy. UI surfaces gather intent and display state, but they do not perform allow/block evaluation.

## Consequences

Policy logic remains pure Kotlin and unit-testable. Android classes, storage implementations, packet parsing, and feature screens must call into policy through narrow interfaces instead of embedding decision logic.
