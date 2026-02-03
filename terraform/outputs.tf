output "api_gateway_url" {
  description = "API Gateway endpoint URL"
  value       = "${aws_apigatewayv2_api.order_api.api_endpoint}/orders"
}

output "dynamodb_table_name" {
  description = "DynamoDB table name"
  value       = aws_dynamodb_table.orders.name
}

output "s3_bucket_name" {
  description = "S3 bucket name"
  value       = aws_s3_bucket.order_archives.bucket
}

output "sns_topic_arn" {
  description = "SNS topic ARN"
  value       = aws_sns_topic.order_notifications.arn
}

output "lambda_function_name" {
  description = "Lambda function name"
  value       = aws_lambda_function.order_processor.function_name
}
