package dev.jefersonsiqueira.clinicflow.common.aws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.net.URI;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Plain AWS SDK v2 clients, not a Quarkus/Quarkiverse extension — this
 * project targets Quarkus 3.39, and the Quarkiverse amazon-services
 * extensions' Dev Services support did not line up with it cleanly enough to
 * be worth chasing version compatibility over; a hand-produced client is
 * three CDI beans, not a new build-time extension dependency to keep in
 * sync. The tradeoff: no Dev Services auto-provisioning of LocalStack the
 * way {@code quarkus-jdbc-postgresql} auto-provisions Postgres — LocalStack
 * is started explicitly via {@code docker-compose.yml} instead (see
 * README's Infrastructure section).
 *
 * <p>Constructing a client here never makes a network call — the AWS SDK is
 * lazy about that — so producing all three unconditionally, even when
 * {@code clinic.aws.enabled=false}, is safe. {@link
 * dev.jefersonsiqueira.clinicflow.notification.ExamReportPublisher} is the
 * one thing that actually calls them, and it checks the flag itself before
 * doing so.
 */
@ApplicationScoped
public class AwsClients {

  @ConfigProperty(name = "clinic.aws.region", defaultValue = "us-east-1")
  String region;

  /** Unset in %prod on purpose — a real deployment has no LocalStack to point at. */
  @ConfigProperty(name = "clinic.aws.endpoint-override")
  java.util.Optional<String> endpointOverride;

  private AwsCredentialsProvider credentialsProvider() {
    // LocalStack does not validate credentials at all, but the SDK still
    // requires *some* provider to be configured — "test"/"test" is
    // LocalStack's own documented convention for this. A real deployment
    // (clinic.aws.enabled=false, so this path is never exercised today)
    // would use the default chain instead: environment variables, an
    // instance profile, whatever the platform actually provides.
    return endpointOverride.isPresent()
        ? StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test"))
        : DefaultCredentialsProvider.builder().build();
  }

  @Produces
  @Singleton
  public S3Client s3Client() {
    var builder = S3Client.builder().region(Region.of(region)).credentialsProvider(credentialsProvider());
    endpointOverride.ifPresent(url -> builder.endpointOverride(URI.create(url)).forcePathStyle(true));
    return builder.build();
  }

  @Produces
  @Singleton
  public SnsClient snsClient() {
    var builder = SnsClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider());
    endpointOverride.ifPresent(url -> builder.endpointOverride(URI.create(url)));
    return builder.build();
  }

  @Produces
  @Singleton
  public SqsClient sqsClient() {
    var builder = SqsClient.builder().region(Region.of(region)).credentialsProvider(credentialsProvider());
    endpointOverride.ifPresent(url -> builder.endpointOverride(URI.create(url)));
    return builder.build();
  }
}
