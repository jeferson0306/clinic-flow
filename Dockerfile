# Multi-stage: Render's Blueprint builds directly from a clean git checkout,
# not from a pre-built target/ directory the way a two-step CI pipeline
# (build the jar, then docker build) would. Stage one runs the same Maven
# build CI does; stage two is Quarkus's own generated runtime image, unchanged.

FROM eclipse-temurin:25-jdk AS build
WORKDIR /build

# eclipse-temurin ships neither curl/wget nor unzip. The Maven Wrapper needs
# curl to fetch its pinned distribution, which is unremarkable — but missing
# unzip makes it silently switch from the pinned .zip to a .tar.gz of the same
# release instead, without recomputing the checksum it then verifies against:
# the pinned SHA-256 is for the .zip, the file actually downloaded is the
# .tar.gz, and they legitimately differ. The result reads as "your Maven
# distribution might be compromised" — externally re-verifying the .zip's own
# checksum against Maven Central (it matched) is what ruled that out and
# pointed at the fallback instead. Installing unzip removes the fallback.
RUN apt-get update && apt-get install -y --no-install-recommends curl unzip \
    && rm -rf /var/lib/apt/lists/*

# Dependencies first, in their own layer: a source change should not
# re-download the whole dependency tree on every build.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B package -DskipTests

FROM registry.access.redhat.com/ubi9/openjdk-25-runtime:1.24
ENV LANGUAGE='en_US:en'
COPY --from=build --chown=185 /build/target/quarkus-app/lib/ /deployments/lib/
COPY --from=build --chown=185 /build/target/quarkus-app/*.jar /deployments/
COPY --from=build --chown=185 /build/target/quarkus-app/app/ /deployments/app/
COPY --from=build --chown=185 /build/target/quarkus-app/quarkus/ /deployments/quarkus/
EXPOSE 8080
USER 185
ENV JAVA_OPTS_APPEND="-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager"
ENV JAVA_APP_JAR="/deployments/quarkus-run.jar"
ENTRYPOINT [ "/opt/jboss/container/java/run/run-java.sh" ]
