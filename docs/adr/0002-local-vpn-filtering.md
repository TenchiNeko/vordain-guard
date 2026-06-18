# ADR 0002: Local VPN Filtering

## Decision

Start with local VPN filtering on the child device instead of routing all traffic through Vordain servers.

## Context

The product requires privacy-preserving enforcement. Sending traffic through Vordain servers would increase privacy risk, compliance burden, infrastructure cost, and blast radius.

## Consequences

The child device observes traffic metadata locally and forwards decisions through the VPN engine. The backend, when added later, acts as a blind encrypted relay and does not receive readable child activity.
