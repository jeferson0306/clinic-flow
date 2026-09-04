package dev.jefersonsiqueira.clinicflow.notification;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Stands in for the real notification service a clinic would actually run
 * (email/SMS to the patient) — polling the queue Terraform subscribes to
 * {@link ExamReportPublisher}'s SNS topic and logging what it would have
 * sent. This is the "something downstream reacts to it" half of the
 * fan-out; {@link ExamReportPublisher} is the "something happened" half,
 * and neither knows the other exists — SNS/SQS is the whole point of not
 * wiring them together directly.
 */
@ApplicationScoped
public class ExamNotificationConsumer {

  static final String QUEUE_NAME = "exam-result-notifications-queue";

  @Inject SqsClient sqs;

  @ConfigProperty(name = "clinic.aws.enabled", defaultValue = "false")
  boolean enabled;

  private String queueUrl;

  @Scheduled(every = "10s")
  void poll() {
    if (!enabled) {
      return;
    }
    try {
      if (queueUrl == null) {
        queueUrl = sqs.getQueueUrl(b -> b.queueName(QUEUE_NAME)).queueUrl();
      }
      List<Message> messages =
          sqs.receiveMessage(
                  ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(10).waitTimeSeconds(1).build())
              .messages();
      for (Message message : messages) {
        Log.infof("exam notification received: %s", message.body());
        sqs.deleteMessage(
            DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(message.receiptHandle()).build());
      }
    } catch (QueueDoesNotExistException e) {
      // Terraform has not been applied yet against this LocalStack instance
      // — not an error worth logging every 10 seconds while someone is
      // still setting up their local environment.
      queueUrl = null;
    } catch (RuntimeException e) {
      Log.warnf(e, "exam notification poll failed");
    }
  }
}
