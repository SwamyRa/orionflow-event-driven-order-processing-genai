resource "aws_cloudwatch_dashboard" "order_processing" {
  dashboard_name = "${var.project_name}-dashboard-${var.environment}"

  dashboard_body = jsonencode({
    widgets = [
      {
        type = "metric"
        x    = 0
        y    = 0
        width = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/Lambda", "Invocations", { stat = "Sum", label = "Total Orders" }],
            [".", "Errors", { stat = "Sum", label = "Failed Orders" }],
            [".", "Throttles", { stat = "Sum", label = "Throttled Requests" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Lambda Invocations & Errors"
          period  = 300
          yAxis = {
            left = {
              min = 0
            }
          }
        }
      },
      {
        type = "metric"
        x    = 12
        y    = 0
        width = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/Lambda", "Duration", { stat = "Average", label = "Avg Duration" }],
            ["...", { stat = "Maximum", label = "Max Duration" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Lambda Duration (ms)"
          period  = 300
          yAxis = {
            left = {
              min = 0
            }
          }
        }
      },
      {
        type = "metric"
        x    = 0
        y    = 6
        width = 8
        height = 6
        properties = {
          metrics = [
            ["OrderProcessing", "OrdersProcessed", "Status", "APPROVED", { stat = "Sum", label = "Approved", color = "#2ca02c" }],
            ["...", "REJECTED", { stat = "Sum", label = "Rejected", color = "#d62728" }],
            ["...", "PENDING_REVIEW", { stat = "Sum", label = "Pending Review", color = "#ff7f0e" }]
          ]
          view    = "timeSeries"
          stacked = true
          region  = var.aws_region
          title   = "Orders by Status"
          period  = 300
          yAxis = {
            left = {
              min = 0
            }
          }
        }
      },
      {
        type = "metric"
        x    = 8
        y    = 6
        width = 8
        height = 6
        properties = {
          metrics = [
            ["OrderProcessing", "FraudScore", { stat = "Average", label = "Avg Fraud Score" }],
            ["...", { stat = "Minimum", label = "Min Score" }],
            ["...", { stat = "Maximum", label = "Max Score" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "AI Fraud Scores"
          period  = 300
          yAxis = {
            left = {
              min = 0
              max = 10
            }
          }
        }
      },
      {
        type = "metric"
        x    = 16
        y    = 6
        width = 8
        height = 6
        properties = {
          metrics = [
            ["OrderProcessing", "ProcessingCost", { stat = "Sum", label = "Total Cost ($)" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Processing Costs"
          period  = 300
          yAxis = {
            left = {
              min = 0
            }
          }
        }
      },
      {
        type = "metric"
        x    = 0
        y    = 12
        width = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/DynamoDB", "ConsumedReadCapacityUnits", "TableName", "${var.project_name}-orders-${var.environment}", { stat = "Sum", label = "Read Units" }],
            [".", "ConsumedWriteCapacityUnits", ".", ".", { stat = "Sum", label = "Write Units" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "DynamoDB Capacity Units"
          period  = 300
          yAxis = {
            left = {
              min = 0
            }
          }
        }
      },
      {
        type = "metric"
        x    = 12
        y    = 12
        width = 12
        height = 6
        properties = {
          metrics = [
            ["AWS/Bedrock", "Invocations", { stat = "Sum", label = "Bedrock API Calls" }],
            ["OrderProcessing", "BedrockTokens", { stat = "Sum", label = "Tokens Used" }]
          ]
          view    = "timeSeries"
          stacked = false
          region  = var.aws_region
          title   = "Bedrock Usage"
          period  = 300
          yAxis = {
            left = {
              min = 0
            }
          }
        }
      },
      {
        type = "log"
        x    = 0
        y    = 18
        width = 24
        height = 6
        properties = {
          query   = "SOURCE '/aws/lambda/order-processor'\n| fields @timestamp, @message\n| filter @message like /APPROVED|REJECTED/\n| sort @timestamp desc\n| limit 20"
          region  = var.aws_region
          title   = "Recent Order Processing Logs"
          stacked = false
        }
      },
      {
        type = "metric"
        x    = 0
        y    = 24
        width = 12
        height = 3
        properties = {
          metrics = [
            ["OrderProcessing", "OrdersProcessed", { stat = "Sum", label = "Total Orders Processed" }]
          ]
          view    = "singleValue"
          region  = var.aws_region
          title   = "Total Orders (24h)"
          period  = 86400
        }
      },
      {
        type = "metric"
        x    = 12
        y    = 24
        width = 12
        height = 3
        properties = {
          metrics = [
            ["OrderProcessing", "ProcessingCost", { stat = "Sum", label = "Total Processing Cost" }]
          ]
          view    = "singleValue"
          region  = var.aws_region
          title   = "Total Cost (24h)"
          period  = 86400
        }
      }
    ]
  })
}

output "cloudwatch_dashboard_url" {
  description = "CloudWatch Dashboard URL"
  value       = "https://console.aws.amazon.com/cloudwatch/home?region=${var.aws_region}#dashboards:name=${aws_cloudwatch_dashboard.order_processing.dashboard_name}"
}
