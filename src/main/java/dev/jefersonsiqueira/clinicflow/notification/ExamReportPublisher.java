package dev.jefersonsiqueira.clinicflow.notification;

import dev.jefersonsiqueira.clinicflow.exam.Exam;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.charset.StandardCharsets;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

/**
 * Archives an exam's result to S3 and publishes an SNS notification the
 * moment {@code ExamService.recordResult} sets one — the fan-out point a
 * real clinic needs: the report has to be retrievable independently of this
 * API's own database, and something downstream (a notification service that
 * would email or SMS the patient — {@link ExamNotificationConsumer} stands
 * in for it here, reading off the SQS queue this SNS topic feeds) needs to
 * hear about it without ExamService knowing or caring who that is.
 *
 * <p>Best-effort, deliberately: a failure here is logged and swallowed, not
 * rethrown. The result is already committed to Postgres by the time this
 * runs — that write is the one that must not be undone by an unrelated S3
 * or SNS outage. Same tradeoff {@code AddressLookupService} makes for
 * ViaCEP, for the same reason: a courtesy should not be allowed to break the
 * thing it is a courtesy to.
 */
@ApplicationScoped
public class ExamReportPublisher {

  /** Matches the S3 bucket Terraform creates — see infra/terraform/main.tf. */
  static final String BUCKET = "clinic-flow-exam-reports";

  /** Matches the SNS topic Terraform creates — see infra/terraform/main.tf. */
  static final String TOPIC_ARN = "arn:aws:sns:us-east-1:000000000000:exam-result-notifications";

  @Inject S3Client s3;
  @Inject SnsClient sns;

  @ConfigProperty(name = "clinic.aws.enabled", defaultValue = "false")
  boolean enabled;

  public void publish(Exam exam) {
    if (!enabled) {
      return;
    }
    try {
      String key = "exams/%s.txt".formatted(exam.id);
      String report =
          """
          Exam report
          -----------
          Exam id: %s
          Patient id: %s
          Requested by (doctor id): %s
          Type: %s
          Requested at: %s
          Result recorded at: %s

          Result:
          %s
          """
              .formatted(
                  exam.id, exam.patientId, exam.requestedByDoctorId, exam.type,
                  exam.requestedAt, exam.resultRecordedAt, exam.result);

      s3.putObject(
          PutObjectRequest.builder().bucket(BUCKET).key(key).contentType("text/plain").build(),
          RequestBody.fromString(report, StandardCharsets.UTF_8));

      sns.publish(
          PublishRequest.builder()
              .topicArn(TOPIC_ARN)
              .subject("Exam result ready")
              .message(
                  "Exam %s for patient %s is ready. Report: s3://%s/%s"
                      .formatted(exam.id, exam.patientId, BUCKET, key))
              .build());
    } catch (RuntimeException e) {
      // Logged, not rethrown — see this class's own javadoc for why. The
      // exam's result is already committed; this is an archival/notification
      // side effect, not part of that transaction's own correctness.
      Log.warnf(e, "failed to publish exam report for exam %s — result was still recorded", exam.id);
    }
  }
}
