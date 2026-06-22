# Project deep-dive — Monolith to modular services migration

## Context

Led a two-year migration at Example Corp from a single Spring monolith to independently deployable Quarkus services.

## Approach

- Strangled the monolith along domain boundaries (billing, identity, documents).
- Introduced contract tests and Testcontainers-backed integration suites before cutting over traffic.
- Ran canary releases on Kubernetes with automated rollback on error-rate SLO breaches.

## Outcomes

- Deploy frequency increased from monthly to multiple times per week.
- P95 API latency for document search dropped by roughly 40% after extracting the retrieval path.
