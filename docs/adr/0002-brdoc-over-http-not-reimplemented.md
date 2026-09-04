# ADR-0002: Document validation via brdoc over HTTP, not reimplemented in Java

## Status
Accepted

## Context
clinic-flow needs to validate CPF, email, telephone and postcode format on
every patient (and later, doctor) record. `brdoc` already does exactly this,
is deployed and reachable, has its check-digit algorithms tested against
published vectors and a reference implementation, and has a production
history — including bugs found and fixed in that history.

## Decision
`clinic-flow` calls `brdoc`'s `/v1/validate` over HTTP (see
`validation/brdoc/`) instead of reimplementing CPF/email/phone checks in
Java.

## Consequences
- One implementation of "is this CPF valid", not two in two languages that
  can silently drift apart — the exact failure mode `dev-standards` exists to
  prevent.
- A network dependency on every write that touches a document. Mitigated,
  not eliminated: short timeouts (`quarkus.rest-client.brdoc.*`), and brdoc
  itself measured a CPF check at ~0.5µs against a 150ms+ Redis round trip
  (see brdoc's own README) — the actual cost here is Render's network hop,
  not brdoc's own work.
- brdoc's free-tier cold start becomes clinic-flow's cold start too, on the
  first request after either has been idle. Both sleep on the same platform,
  so this is not a new failure mode introduced by the dependency — the
  ceiling was already there.
- If brdoc is ever unreachable, patient registration fails outright rather
  than falling back to no validation — a document silently accepted unchecked
  is worse than a registration that has to be retried.
