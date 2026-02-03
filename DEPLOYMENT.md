# Deployment Guide

## Prerequisites

1. **AWS Account** with permissions for:
   - Lambda, API Gateway, DynamoDB, S3, SNS, CloudWatch
   - EKS, ECR, VPC, IAM
   - Cost Explorer API access

2. **GitHub Repository** with code pushed

3. **GitHub Secrets** - Configure in repo Settings > Secrets and variables > Actions:
   ```
   AWS_ACCESS_KEY_ID=<your-access-key>
   AWS_SECRET_ACCESS_KEY=<your-secret-key>
   SNS_EMAIL=<your-email-for-notifications>
   ```

## Deployment Steps

### 1. Create ECR Repository (One-time)
```bash
aws ecr create-repository \
  --repository-name eks-analytics \
  --region us-east-1
```

### 2. Push Code to GitHub
```bash
git add .
git commit -m "Initial commit"
git push origin main
```

### 3. GitHub Actions Auto-Deploy
The pipeline automatically:
- ✅ Builds Lambda JAR
- ✅ Builds Docker image
- ✅ Pushes to ECR
- ✅ Deploys Terraform (all AWS infrastructure)
- ✅ Deploys Helm chart to EKS

### 4. Verify Deployment

**Check Lambda:**
```bash
aws lambda get-function --function-name order-processor
```

**Check EKS:**
```bash
aws eks update-kubeconfig --name order-processing-eks-dev --region us-east-1
kubectl get pods
kubectl get svc
```

**Test Lambda:**
```bash
aws apigateway get-rest-apis --query 'items[?name==`order-processor-api`].id' --output text
# Use API Gateway URL to test
```

**Test EKS Analytics:**
```bash
kubectl port-forward svc/eks-analytics 8080:8080
curl http://localhost:8080/api/finops/metrics
```

## Manual Deployment (Optional)

If you prefer manual deployment without GitHub Actions:

**Deploy Terraform:**
```bash
cd terraform
terraform init
terraform apply -var="sns_email=your@email.com"
```

**Build and Push Docker:**
```bash
cd eks-analytics
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 418295711730.dkr.ecr.us-east-1.amazonaws.com
docker build -t eks-analytics .
docker tag eks-analytics:latest 418295711730.dkr.ecr.us-east-1.amazonaws.com/eks-analytics:latest
docker push 418295711730.dkr.ecr.us-east-1.amazonaws.com/eks-analytics:latest
```

**Deploy Helm:**
```bash
aws eks update-kubeconfig --name order-processing-eks-dev --region us-east-1
cd eks-analytics/helm
helm upgrade --install eks-analytics . --wait
```

## Cost Monitoring

After deployment, monitor costs:
- **Lambda**: ~$0.01/day (100 requests)
- **EKS**: ~$3/day (cluster + 2 nodes)
- **DynamoDB**: ~$0.50/day (on-demand)
- **S3**: ~$0.01/day
- **Total**: ~$3.50/day or ~$105/month

## Cleanup

```bash
# Delete EKS deployment
helm uninstall eks-analytics

# Destroy infrastructure
cd terraform
terraform destroy -var="sns_email=your@email.com"

# Delete ECR images
aws ecr batch-delete-image \
  --repository-name eks-analytics \
  --image-ids imageTag=latest
```
