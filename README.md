# clinic-flow

A clinic management system — patients, doctors, procedures, exams,
appointments, billing and calendar, one place — built to run a **public
sandbox**: anyone can register and try the real flow, not a screenshot of it.

Java 25, Quarkus 3.39. Backend for a portfolio project; the frontend (GSAP,
animejs, PT/EN/ES) is a separate app consuming this API.

## Status

**Phase 3 — calendar, tracing, virtual threads.** Everything through
appointments (patients, doctors, procedures, exams, double-booking rejected
by Postgres itself) plus the query side scheduling did not need but a
calendar view does: `GET /v1/doctors/{id}/availability` lists a doctor's free
slots for a day and a procedure. Every request carries an OpenTelemetry
trace id through its logs and back as `X-Trace-Id`; every endpoint runs on a
virtual thread rather than one of Quarkus's few event-loop threads.

## Architecture

One deployable, not a service mesh — see
[ADR-0001](docs/adr/0001-modulith-not-microservices.md) for why, given this
has to run on a Render free instance for a demo nobody scheduled in advance.
Modules are packages with real boundaries (`patient`, and whichever of the
roadmap below lands next), not layers spanning the whole domain.

Document validation — CPF, email, phone, postcode format — is not
reimplemented here. `clinic-flow` calls the already-deployed `brdoc` service
over HTTP; see [ADR-0002](docs/adr/0002-brdoc-over-http-not-reimplemented.md).
Postcode *content* (street, city, state) comes from ViaCEP, Brazil's public
postcode lookup, and is never allowed to block registration if it is slow or
down — see `AddressLookupService`.

```
patient/             entity, repository, service, REST resource, DTOs
doctor/               same shape as patient/, plus a licence number
procedure/            the bookable catalogue — no brdoc involvement, no documents
exam/                 requested against a patient by a doctor, result recorded later
appointment/          patient + doctor + procedure + time slot; the double-booking
                      guarantee lives in the database, not here — see V5's migration
calendar/             the query side of scheduling — AvailabilityCalculator is pure,
                      unit-tested logic; AvailabilityService is where it meets the
                      database and the working-hours config
address/              the embeddable Address value object + the ViaCEP lookup
validation/brdoc/     the brdoc REST client and the DocumentValidator every
                      module validates through
validation/viacep/    the ViaCEP REST client
common/               the one exception mapper every module's errors go through,
                      the CPF-masking helper, and the trace-id response filter
```

## Concurrency and observability

Every resource method runs `@RunOnVirtualThread`: the code is ordinary
blocking Java — JPA, a blocking REST client call to brdoc — and a virtual
thread is what lets that scale like non-blocking code without becoming
non-blocking code. No manual thread-pool tuning, no reactive rewrite of
Hibernate ORM into Hibernate Reactive, no `Uni`/`Multi` chains threaded
through every method signature — the throughput case a reactive stack exists
to make is already made, at a fraction of the complexity.

Every request generates an OpenTelemetry trace id, which reaches every log
line that request produces (`quarkus.log.console.format` in
application.properties) and comes back to the caller as `X-Trace-Id`
(`TraceIdResponseFilter`). One identifier ties a request, its response and
every log line it wrote together — a caller reporting a problem can hand back
that one value. No collector is deployed yet (`quarkus.otel.traces.exporter=none`),
so nothing is exported anywhere; the ids exist and reach the logs regardless.

## Roadmap

Each phase is a real, working increase in scope — not scaffolding for its own
sake. In the order they will be built:

1. ~~**Patients & doctors**~~ — done.
2. ~~**Clinical operations**~~ — done: procedures, exams, appointments, the
   double-booking guarantee.
3. ~~**Calendar**~~ — done: `GET /v1/doctors/{id}/availability?date=...&procedureId=...`.
4. **Billing** — Stripe and Mercado Pago/Pix, in sandbox mode only. Payment
   intents tied to an appointment, webhook handling, idempotent by design —
   a retried webhook must not charge twice.
5. **Public sandbox mode** — pre-seeded demo accounts so a first-time visitor
   has something to look at immediately, rate limiting on public
   self-registration (reusing the lesson from brdoc's own rate limiter), and
   a way to tell demo data apart from anything that matters.
6. **Observability** — structured JSON logs in production
   (`quarkus-logging-json`), request tracing, metrics beyond the Prometheus
   endpoint already wired up, health checks that actually check the
   dependencies (database, brdoc, payment provider) rather than just
   answering 200.
7. **Frontend** — Next.js, GSAP, animejs, i18n in PT/EN/ES, calling this API.
   Deployed separately (Vercel), same split as the validator's own playground.

## Running locally

Needs Java 25 and Docker — [Quarkus Dev Services](https://quarkus.io/guides/dev-services)
starts a real Postgres in a container automatically; there is no
docker-compose file to keep in sync with the schema by hand.

```bash
./mvnw quarkus:dev
```

Swagger UI: http://localhost:8080/q/swagger-ui
Health: http://localhost:8080/q/health

## Testing

Two kinds, kept apart by Maven's own naming convention rather than by hand:

- **Unit tests** (`*Test.java`, run by Surefire) — no Quarkus context, no
  database, no network. `AvailabilityCalculatorTest` is the one so far: pure
  free-slot math in, assertions out, fast enough to run on every save.
- **Integration tests** (`*IT.java`, run by Failsafe) — `@QuarkusTest`
  classes that boot the whole application against a real Postgres, started
  on demand by Dev Services. `brdoc` and ViaCEP are mocked (`@InjectMock`)
  even here — a test run should never depend on a third-party free service
  being awake, and brdoc's own contract is already covered by its own test
  suite. What these own is the wiring: a rejection from brdoc is handled
  correctly, a patient is stored with the *normalized* document values, a
  ViaCEP outage never blocks registration, a doctor is never double-booked.

```bash
./mvnw test      # unit only — seconds, no Docker
./mvnw verify    # unit + integration — what CI runs
```

## Deployment

Not deployed yet. The plan, once this phase is reviewed:

- **Database:** Neon Postgres. `DATABASE_URL`, `DATABASE_USER`,
  `DATABASE_PASSWORD` as Render environment variables — see
  `%prod` in `application.properties`.
- **Backend:** Render, same free tier as `brdoc`.
- **Payments:** Stripe and Mercado Pago in sandbox/test mode only, added when
  the billing phase starts — no key for either exists yet.
