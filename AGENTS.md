# AGENTS.md

## Project overview

Java 25, Quarkus 3.39, a clinic management system built as a single
deployable (a modulith — see `docs/adr/0001-modulith-not-microservices.md`).
Document validation (CPF, email, phone, postcode format) is delegated to the
deployed [brdoc](https://github.com/jeferson0306/brdoc) service over HTTP
rather than reimplemented — see `docs/adr/0002-brdoc-over-http-not-reimplemented.md`.
`README.md` has the full roadmap; `patient`, `doctor`, `procedure`, `exam`,
`appointment` and `calendar` exist so far. Every resource runs on a virtual
thread (`@RunOnVirtualThread`); every request carries an OpenTelemetry trace
id into its logs and back as `X-Trace-Id`.

## Running

```bash
./mvnw quarkus:dev   # needs Docker — Dev Services starts a real Postgres
./mvnw test           # *Test.java, Surefire — unit only, no Docker, seconds
./mvnw verify          # + *IT.java, Failsafe — @QuarkusTest, real Postgres
```

Naming a new test class `*Test` vs `*IT` is not cosmetic: Surefire and
Failsafe pick them up by that suffix alone (see `pom.xml`'s
`<skipITs>false</skipITs>`), and it decides whether the test needs Docker.

Swagger UI: `http://localhost:8080/q/swagger-ui` · Health: `/q/health`.

### JDK 25

Not necessarily the `java` on `PATH` — Homebrew installs it keg-only. Point
`JAVA_HOME` at it explicitly for local builds:

```bash
export JAVA_HOME="$(brew --prefix openjdk@25)"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Non-obvious notes

- **The `quarkus-opentelemetry` extension pulls a ~600MB Grafana LGTM
  container (Loki/Grafana/Tempo/Mimir) into Dev Services the moment it is on
  the classpath**, even with `quarkus.otel.traces.exporter=none` — that
  property only controls where finished spans are *exported*, not whether
  Dev Services starts a collector to export them to. The actual switch is
  `quarkus.observability.enabled=false`. Found by `quarkus:dev` downloading
  several hundred MB on startup for no reason anything in this project asked
  for.
- **A REST client returning `Response` still throws on brdoc's 422s, by
  itself.** The MicroProfile REST Client spec mandates a default exception
  mapper that fires on any status ≥ 400, and this applies *before* your
  method's return type is even considered — asking for the raw `Response`
  does not exempt you, contrary to what the type would suggest. Three
  documented ways to suppress it were tried against `BrdocClient` and did
  nothing on their own: a custom `ResponseExceptionMapper` at a low priority,
  Quarkus's `@ClientExceptionMapper`, and returning `Response` alone. What
  actually works is
  `quarkus.rest-client."brdoc".disable-default-mapper=true` in
  `application.properties`, **combined with** a `Response` return type — the
  property alone was also tested and did not stop the throw. See
  `BrdocClient`'s javadoc and https://github.com/quarkusio/quarkus/discussions/47556.
- **brdoc and ViaCEP are mocked in tests** (`@InjectMock`), never called for
  real — a CI run should not depend on either being awake. Manual smoke
  testing against the real services is still worth doing before trusting a
  change to `validation/`; that is how two of the notes above were found.
- **Test data must be unique across the whole suite, not just within one
  test method.** `@QuarkusTest` does not roll back between tests, and every
  test class in this project shares one Postgres container for the whole
  `mvn test` run — a fixed CPF or licence number literal reused across test
  *methods*, or across *classes* (`PatientResourceTest` and
  `AppointmentResourceTest` both create a patient, `DoctorResourceTest` and
  `AppointmentResourceTest` both create a doctor), collides on the real
  unique constraint. Generate anything that has to be unique
  (`AppointmentResourceTest.unique()`) rather than hard-coding it.
- **brdoc's timeouts are 5s connect / 20s read, not shorter.** brdoc runs on
  Render's free tier and sleeps after 15 minutes idle, same as this service
  will. A cold start there was clocked taking longer than a short timeout
  tolerates while testing the doctor module manually — 5s turned a slow but
  legitimate registration into a 500. If this keeps being a problem once
  there is real traffic, the fix is keeping brdoc warm (a scheduled ping), not
  a shorter timeout.
- **`ConstraintViolationException.getConstraintName()` returns `null` for a
  Postgres `EXCLUDE` constraint violation.** Hibernate's name extractor
  recognises `UNIQUE` and foreign-key violation message shapes, not
  `EXCLUDE`'s. `AppointmentService`'s double-booking detection checks
  `getSQLState().equals("23P01")` instead — Postgres's standard SQLSTATE for
  an exclusion violation, which does not depend on Hibernate parsing
  anything. Found by logging the actual exception rather than trusting the
  constraint-name check to have worked.
- **A class-level `@Consumes(APPLICATION_JSON)` breaks a bodyless endpoint on
  the same resource with a 415.** `AppointmentResource.cancel()` takes no
  request body; a POST without one does not carry a matching `Content-Type`,
  and JAX-RS enforces the class-wide `@Consumes` against it regardless.
  `@Consumes` now sits only on `schedule()`, the one method that has a body.
  A `GET` with no body is unaffected — this only bit a bodyless `POST`.
- **The CPF in every API response is masked** to its last two digits
  (`common/DocumentMasking.maskCpf`) — this module is reachable from a public sandbox.
  Do not add an endpoint that returns the unmasked value.
