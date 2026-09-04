# AGENTS.md

## Project overview

Java 25, Quarkus 3.39, a clinic management system built as a single
deployable (a modulith — see `docs/adr/0001-modulith-not-microservices.md`).
Document validation (CPF, email, phone, postcode format) is delegated to the
deployed [brdoc](https://github.com/jeferson0306/brdoc) service over HTTP
rather than reimplemented — see `docs/adr/0002-brdoc-over-http-not-reimplemented.md`.
`README.md` has the full roadmap; `patient` and `doctor` exist so far.

## Running

```bash
./mvnw quarkus:dev   # needs Docker — Dev Services starts a real Postgres
./mvnw test           # brdoc and ViaCEP are mocked; no network needed
./mvnw verify          # what CI runs
```

Swagger UI: `http://localhost:8080/q/swagger-ui` · Health: `/q/health`.

### JDK 25

Not necessarily the `java` on `PATH` — Homebrew installs it keg-only. Point
`JAVA_HOME` at it explicitly for local builds:

```bash
export JAVA_HOME="$(brew --prefix openjdk@25)"
export PATH="$JAVA_HOME/bin:$PATH"
```

## Non-obvious notes

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
  change to `validation/`; that is how the note above was found.
- **brdoc's timeouts are 5s connect / 20s read, not shorter.** brdoc runs on
  Render's free tier and sleeps after 15 minutes idle, same as this service
  will. A cold start there was clocked taking longer than a short timeout
  tolerates while testing the doctor module manually — 5s turned a slow but
  legitimate registration into a 500. If this keeps being a problem once
  there is real traffic, the fix is keeping brdoc warm (a scheduled ping), not
  a shorter timeout.
- **The CPF in every API response is masked** to its last two digits
  (`PatientResponse.mask`) — this module is reachable from a public sandbox.
  Do not add an endpoint that returns the unmasked value.
