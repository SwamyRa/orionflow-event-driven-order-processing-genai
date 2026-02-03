resource "aws_s3_bucket" "order_archives" {
  bucket = "${var.project_name}-archives-v2-${var.environment}-${data.aws_caller_identity.current.account_id}"
}

resource "aws_s3_bucket_versioning" "order_archives" {
  bucket = aws_s3_bucket.order_archives.id
  
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_lifecycle_configuration" "order_archives" {
  bucket = aws_s3_bucket.order_archives.id

  rule {
    id     = "archive-old-orders"
    status = "Enabled"

    filter {}

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = 365
    }
  }
}

resource "aws_s3_bucket_server_side_encryption_configuration" "order_archives" {
  bucket = aws_s3_bucket.order_archives.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

data "aws_caller_identity" "current" {}
