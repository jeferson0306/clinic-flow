# Provisions the S3 bucket, SNS topic and SQS queue ExamReportPublisher and
# ExamNotificationConsumer talk to (see their javadoc for the actual use
# case: archiving a recorded exam result and notifying whatever would
# email/SMS the patient about it). Targets LocalStack exclusively — every
# endpoint below points at docker-compose's container, and the "AWS"
# provider credentials are LocalStack's own documented dummy values, not
# real ones. There is no equivalent applied against real AWS; this project
# has no AWS account behind it (see clinic-flow's AwsClients javadoc).
#
# Usage:
#   docker compose up -d        # from the repo root
#   terraform init
#   terraform apply

terraform {
  required_version = ">= 1.5"
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region                      = "us-east-1"
  access_key                  = "test"
  secret_key                  = "test"
  skip_credentials_validation = true
  skip_metadata_api_check     = true
  skip_requesting_account_id  = true

  endpoints {
    s3  = "http://localhost:4566"
    sns = "http://localhost:4566"
    sqs = "http://localhost:4566"
  }
}

# --- S3: exam report archive -------------------------------------------
# Key layout: exams/<exam-id>.txt — see ExamReportPublisher.

resource "aws_s3_bucket" "exam_reports" {
  bucket = "clinic-flow-exam-reports"
}

# --- SNS -> SQS: exam result fan-out ------------------------------------
# ExamReportPublisher publishes to the topic; ExamNotificationConsumer polls
# the queue. Neither Java class references the other — this subscription is
# the only thing connecting them, which is the point of the pattern.

resource "aws_sns_topic" "exam_result_notifications" {
  name = "exam-result-notifications"
}

resource "aws_sqs_queue" "exam_result_notifications_queue" {
  name = "exam-result-notifications-queue"
}

resource "aws_sqs_queue_policy" "allow_sns" {
  queue_url = aws_sqs_queue.exam_result_notifications_queue.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect    = "Allow"
      Principal = { Service = "sns.amazonaws.com" }
      Action    = "sqs:SendMessage"
      Resource  = aws_sqs_queue.exam_result_notifications_queue.arn
      Condition = {
        ArnEquals = { "aws:SourceArn" = aws_sns_topic.exam_result_notifications.arn }
      }
    }]
  })
}

resource "aws_sns_topic_subscription" "queue_subscription" {
  topic_arn = aws_sns_topic.exam_result_notifications.arn
  protocol  = "sqs"
  endpoint  = aws_sqs_queue.exam_result_notifications_queue.arn
}
