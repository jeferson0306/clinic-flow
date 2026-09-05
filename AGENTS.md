# AGENTS.md

## Project overview

Java 25, Quarkus 3.39, a clinic management system built as a single
deployable (a modulith — see `docs/adr/0001-modulith-not-microservices.md`).
Document validation (CPF, email, phone, postcode format) is delegated to the
deployed [brdoc](https://github.com/jeferson0306/brdoc) service over HTTP
rather than reimplemented — see `docs/adr/0002-brdoc-over-http-not-reimplemented.md`.
`README.md` has the full roadmap; `patient`, `doctor`, `procedure`, `exam`,
`appointment`, `calendar`, `ratelimit` and `auth` exist so far. Every
resource runs on a virtual thread (`@RunOnVirtualThread`); every request
carries an OpenTelemetry trace id into its logs and back as `X-Trace-Id`,
and is throttled per client address by a token bucket (`ratelimit/`) before
it reaches routing. Every `POST` requires a JWT with the right role
(`@RolesAllowed`) — two seeded accounts, `admin`/`admin123` and
`doctor`/`doctor123`, exist for `POST /v1/auth/login` from the first deploy.
Never commit a real (non-`%dev`/`%test`) JWT private key — see
`README.md`'s Authentication section for how production's is handled
instead.

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

- **A Docker build failing the Maven Wrapper's checksum with "your Maven
  distribution might be compromised" is not necessarily what it says.** If
  the base image lacks `unzip`, the wrapper silently falls back from its
  pinned `.zip` to a `.tar.gz` of the same release, and does not recompute
  the checksum it then verifies against — the pin is for the `.zip`, the
  file downloaded is the `.tar.gz`, and they legitimately differ. Verify the
  pinned SHA-256 against Maven Central independently before assuming the
  distribution itself is bad; `Dockerfile` installs `unzip` alongside `curl`
  for exactly this reason.
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
  `*IT` class shares one Postgres container for the whole Failsafe run — a
  fixed CPF or licence number literal reused across test *methods*, or
  across *classes* (`PatientResourceIT` and `AppointmentResourceIT` both
  create a patient, `DoctorResourceIT` and `AppointmentResourceIT` both
  create a doctor), collides on the real unique constraint. Generate
  anything that has to be unique (`AppointmentResourceIT.unique()`) rather
  than hard-coding it.
- **The same sharing applies to `RateLimitFilter`'s buckets, keyed by client
  address.** Every `*IT` class also shares one JVM and therefore one address
  (loopback) — dozens of unrelated test methods drawing against the same
  bucket the way real callers behind one NAT would. `%test.clinic.rate-limit.*`
  is set to a number the whole suite will never reach; the one test that
  actually exercises throttling, `RateLimitFilterIT`, overrides it back down
  via `@TestProfile`, which boots that class its own, separate application
  context rather than sharing the rest of the suite's.
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
- **Every error is `{field, message, category, traceId, timestamp, path}`**,
  built once in `GlobalExceptionMapper.error()` — a new branch in that switch
  should call it, not construct `ApiError` by hand, or it will be missing the
  four fields that make an error actually answerable from logs alone. `field`
  is `null` when there is no single offending field (a 404, a 500).
- **A single, unnamed `@ExampleObject` is silently dropped from the generated
  OpenAPI document.** SmallRye needs a `name`, even when there is only one
  example for that response — verify against `/q/openapi` directly (or
  `curl .../q/openapi | jq`), not just that the Java annotations compile.
- **A class-level `@TestSecurity` cannot be "removed" for one test method.**
  It intercepts every request in that class and injects a fake principal
  regardless of what was actually sent — a test inside that class can never
  see what happens with no credentials at all. `AuthorizationIT` is
  deliberately the one `*IT` class with no class-level `@TestSecurity`, for
  exactly that reason; every other class's blanket
  `@TestSecurity(roles = {"ADMIN", "DOCTOR"})` is only safe *because*
  nothing in it is testing the RBAC boundary itself.
- **Bcrypt needs the classpath it needs.** `BcryptUtil.bcryptHash`/`matches`
  throw `NoClassDefFoundError: org/wildfly/common/Assert` if invoked outside
  a full Quarkus (or at least full Maven dependency) classpath — a bare
  `jshell --class-path <one jar>` is not enough; use
  `mvn dependency:build-classpath` to get the real one first.
- **LocalStack's SNS ARN and SQS URL are predictable, not returned by
  anything this app calls.** Account id `000000000000`, region from
  `clinic.aws.region` — `ExamReportPublisher.TOPIC_ARN` is a hardcoded
  string built from exactly that, matching the resource names Terraform
  creates in `infra/terraform/main.tf`. Rename either side and the two
  silently stop matching; nothing catches it until a publish 404s.
- **Port 4566 is LocalStack's own default, which makes it a common
  collision on a machine already running another project's LocalStack.**
  Found this exact collision testing S3/SNS/SQS integration by hand —
  `docker compose up -d` failed with "port is already allocated" because an
  unrelated project's LocalStack container already held it. The two are
  otherwise independent (LocalStack namespaces resources by name within one
  instance), so pointing at whichever instance already has the port during
  manual verification is fine; just don't tear down a container this
  project did not start.
- **AWS SDK v2 clients are lazy — constructing one never makes a network
  call.** `AwsClients` produces `S3Client`/`SnsClient`/`SqsClient`
  unconditionally regardless of `clinic.aws.enabled`, and that is safe for
  exactly this reason; only `ExamReportPublisher`/`ExamNotificationConsumer`
  actually invoking an operation would reach the network, and both check the
  flag themselves before doing so.
- **There is no `secretFiles` field in the Render Blueprint spec**, despite
  it looking exactly like the kind of thing that should exist next to
  `envVars`' `sync: false`. `render.yaml` had one; the Blueprint apply
  failed with a generic "there was an issue" and no field-level detail —
  confirmed by checking Render's actual docs page, which never mentions
  `secretFiles` at all. The JWT private key has to be added by hand, after
  the service exists, under Settings → Secret Files.
