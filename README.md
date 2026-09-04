# clinic-flow

A clinic management system — patients, doctors, procedures, exams,
appointments, billing and calendar, one place — built to run a **public
sandbox**: anyone can register and try the real flow, not a screenshot of it.

Java 25, Quarkus 3.39. Backend for a portfolio project; the frontend (GSAP,
animejs, PT/EN/ES) is a separate app consuming this API.

## Status

**Phase 0 — architecture and one vertical slice.** Patient registration is
real end to end: validated against [brdoc](https://github.com/jeferson0306/brdoc)
over HTTP, address auto-filled from ViaCEP, stored in Postgres via Flyway-
managed schema, CPF masked in every response. Nothing else in the roadmap
below exists yet.

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
patient/            entity, repository, service, REST resource, DTOs
address/            the embeddable Address value object + the ViaCEP lookup
validation/brdoc/   the brdoc REST client and the DocumentValidator every
                     module validates through
validation/viacep/  the ViaCEP REST client
common/             the one exception mapper every module's errors go through
```

## Roadmap

Each phase is a real, working increase in scope — not scaffolding for its own
sake. In the order they will be built:

1. **Patients & doctors** *(patients: done)* — doctor registration,
   specialties, both validated the same way.
2. **Clinical operations** — procedure catalogue, exam records, appointments
   tying patient + doctor + procedure + time slot together, with a doctor
   double-booking rejected at the database, not just in application code.
3. **Calendar** — availability per doctor, working hours, the booking API the
   frontend's calendar view calls.
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

`brdoc` and ViaCEP are mocked in tests (`@InjectMock`) — a CI run should never
depend on a third-party free service being awake, and brdoc's own contract is
already covered by its own test suite. What these tests own is: a rejection
from brdoc is handled correctly, a patient is stored with the *normalized*
document values, and a ViaCEP outage never blocks registration.

```bash
./mvnw test
```

## Deployment

Not deployed yet. The plan, once Phase 0 is reviewed:

- **Database:** Neon Postgres. `DATABASE_URL`, `DATABASE_USER`,
  `DATABASE_PASSWORD` as Render environment variables — see
  `%prod` in `application.properties`.
- **Backend:** Render, same free tier as `brdoc`.
- **Payments:** Stripe and Mercado Pago in sandbox/test mode only, added when
  the billing phase starts — no key for either exists yet.
