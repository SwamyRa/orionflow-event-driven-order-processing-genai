variable "aws_region" {
  description = "AWS region"
  type        = string
  default     = "us-east-1"
}

variable "environment" {
  description = "Environment name"
  type        = string
  default     = "dev"
}

variable "project_name" {
  description = "Project name"
  type        = string
  default     = "order-processing"
}

variable "notification_email" {
  description = "Email for SNS notifications"
  type        = string
}

variable "bedrock_model_id" {
  description = "Bedrock model ID for fraud detection"
  type        = string
  default     = "amazon.nova-micro-v1:0"
}

variable "lambda_memory" {
  description = "Lambda memory in MB"
  type        = number
  default     = 512
}

variable "lambda_timeout" {
  description = "Lambda timeout in seconds"
  type        = number
  default     = 60
}
