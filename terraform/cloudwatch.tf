resource "aws_cloudwatch_metric_alarm" "lambda_errors" {
  alarm_name          = "${var.project_name}-lambda-errors-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "Errors"
  namespace           = "AWS/Lambda"
  period              = 300
  statistic           = "Sum"
  threshold           = 5
  alarm_description   = "Alert when Lambda errors exceed threshold"
  alarm_actions       = [aws_sns_topic.order_notifications.arn]

  dimensions = {
    FunctionName = aws_lambda_function.order_processor.function_name
  }
}

resource "aws_cloudwatch_metric_alarm" "high_cost_orders" {
  alarm_name          = "${var.project_name}-high-cost-orders-${var.environment}"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = 1
  metric_name         = "OrderProcessingCost"
  namespace           = "OrderProcessing/FinOps"
  period              = 300
  statistic           = "Average"
  threshold           = 0.01
  alarm_description   = "Alert when order processing cost is high"
  alarm_actions       = [aws_sns_topic.order_notifications.arn]
}
