# Terraform Remote State Backend
# 
# SETUP (one-time):
# 1. Create S3 bucket:
#    aws s3 mb s3://order-processing-terraform-state-418295711730 --region us-east-1
#
# 2. Enable versioning:
#    aws s3api put-bucket-versioning --bucket order-processing-terraform-state-418295711730 --versioning-configuration Status=Enabled
#
# 3. Create DynamoDB table for state locking:
#    aws dynamodb create-table \
#      --table-name terraform-state-lock \
#      --attribute-definitions AttributeName=LockID,AttributeType=S \
#      --key-schema AttributeName=LockID,KeyType=HASH \
#      --billing-mode PAY_PER_REQUEST \
#      --region us-east-1
#
# 4. Uncomment backend block in main.tf
# 5. Run: terraform init -migrate-state

terraform {
  backend "s3" {
    bucket         = "order-processing-terraform-state-418295711730"
    key            = "order-processing/terraform.tfstate"
    region         = "us-east-1"
    dynamodb_table = "terraform-state-lock"
    encrypt        = true
  }
}
