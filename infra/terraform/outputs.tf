output "exam_reports_bucket" {
  value = aws_s3_bucket.exam_reports.bucket
}

output "exam_result_notifications_topic_arn" {
  value = aws_sns_topic.exam_result_notifications.arn
}

output "exam_result_notifications_queue_url" {
  value = aws_sqs_queue.exam_result_notifications_queue.id
}
