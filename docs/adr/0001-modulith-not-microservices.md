# ADR-0001: A Quarkus modulith, not microservices

## Status
Accepted

## Context
`travel-platform` — the reference for what "production practice" means in this
portfolio — is nine independently deployable microservices behind a gateway,
with Kafka joining them. That shape is right for demonstrating distributed-
systems practice, and it stays as the reference for it.

clinic-flow has a different constraint: it needs to run a **public sandbox**
that a stranger can register into and try, on a Render free-tier instance that
sleeps after 15 minutes of no traffic. Every additional service is another
cold start, another datastore connection budget, another thing to keep awake
for a demo nobody scheduled in advance.

## Decision
One deployable: a single Quarkus application, internally organised into
package-level modules with real boundaries (`patient`, `appointment`,
`billing`, …) rather than layers (`controller`, `service`, `repository`
spanning the whole domain). A module talks to another module's public API
only — its entities and repository stay unexported outside the package.

## Consequences
- One cold start, one connection pool, one thing to keep alive for the demo.
- No network hop, no serialization, no partial-failure handling between
  modules that belong to the same request — an appointment touching a
  patient and a doctor is one transaction, not a saga.
- The boundary discipline has to be enforced by convention and code review,
  not by a network in between. If a module starts reaching into another's
  entity classes directly, that is the signal this decision needs revisiting
  — not a reason to have started with services.
- If a module's load profile ever actually diverges from the rest (billing
  under PCI scope, say), it is a candidate to split out later. Splitting a
  well-bounded module out is a known, mechanical move; a service split apart
  from a mess of shared tables is not.
