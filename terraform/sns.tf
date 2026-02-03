resource "aws_sns_topic" "order_notifications" {
  name = "${var.project_name}-notifications-${var.environment}"
}

resource "aws_sns_topic_subscription" "email" {
  topic_arn = aws_sns_topic.order_notifications.arn
  protocol  = "email"
  endpoint  = var.notification_email
}
