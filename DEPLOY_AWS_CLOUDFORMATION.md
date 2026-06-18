# Deploy to AWS with CloudFormation (ECS Fargate)

This guide deploys your Spring Boot API to AWS using:

- Amazon ECR (container image)
- Amazon ECS Fargate (runtime)
- Application Load Balancer (public URL)
- IAM Task Role (DynamoDB access)
- CloudFormation (infrastructure as code)

## Prerequisites

- AWS CLI configured (`aws configure`)
- Docker Desktop installed and running
- Existing VPC with at least 2 public subnets in the same region
- Existing DynamoDB table (default: `records`)

## Files Added

- `Dockerfile`
- `.dockerignore`
- `infra/cloudformation/ecs-fargate.yml`
- `infra/cloudformation/parameters-example.json`

## 1) Set variables in PowerShell

```powershell
$Region = "us-east-1"
$AccountId = aws sts get-caller-identity --query Account --output text
$RepositoryName = "dynamodbpoc"
$ImageTag = "latest"
$StackName = "dynamodbpoc-stack"
```

## 2) Create ECR repository (one-time)

```powershell
aws ecr create-repository --repository-name $RepositoryName --region $Region
```

If it already exists, AWS will return an error that can be ignored.

## 3) Build and push Docker image

```powershell
$EcrUri = "$AccountId.dkr.ecr.$Region.amazonaws.com/$RepositoryName"
aws ecr get-login-password --region $Region | docker login --username AWS --password-stdin "$AccountId.dkr.ecr.$Region.amazonaws.com"
docker build -t "$RepositoryName`:$ImageTag" .
docker tag "$RepositoryName`:$ImageTag" "$EcrUri`:$ImageTag"
docker push "$EcrUri`:$ImageTag"
```

## 4) Update CloudFormation parameters

Open `infra/cloudformation/parameters-example.json` and set:

- `VpcId`
- `PublicSubnet1`
- `PublicSubnet2`
- `ContainerImage` to your pushed image URI (for example `123456789012.dkr.ecr.us-east-1.amazonaws.com/dynamodbpoc:latest`)
- `DynamoTableName` if not `records`

## 5) Deploy the stack

```powershell
aws cloudformation deploy `
  --stack-name $StackName `
  --template-file "infra/cloudformation/ecs-fargate.yml" `
  --parameter-overrides file://infra/cloudformation/parameters-example.json `
  --capabilities CAPABILITY_NAMED_IAM `
  --region $Region
```

## 6) Get URLs from stack outputs

```powershell
aws cloudformation describe-stacks `
  --stack-name $StackName `
  --region $Region `
  --query "Stacks[0].Outputs[].[OutputKey,OutputValue]" `
  --output table
```

Use these outputs:

- `ServiceUrl`
- `SwaggerUrl`
- `ApiDocsUrl`

## 7) Smoke test deployed API

```powershell
$ServiceUrl = "<paste ServiceUrl output>"
$body = '{"name":"CFN Deploy Test","description":"Created after AWS deploy"}'
Invoke-RestMethod -Method Post -Uri "$ServiceUrl/api/records" -ContentType "application/json" -Body $body
Invoke-RestMethod -Method Get -Uri "$ServiceUrl/api/records"
```

## Update deployment after code changes

```powershell
docker build -t "$RepositoryName`:$ImageTag" .
docker tag "$RepositoryName`:$ImageTag" "$EcrUri`:$ImageTag"
docker push "$EcrUri`:$ImageTag"
aws ecs update-service --cluster dynamodb-poc-cluster --service dynamodb-poc-svc --force-new-deployment --region $Region
```

## Notes

- The task role grants table-level permissions for `DescribeTable`, `Scan`, `GetItem`, `PutItem`, `UpdateItem`, `DeleteItem`.
- `AWS_DYNAMODB_PROFILE` is set empty in ECS so the app uses task-role credentials.
- If you want HTTPS + domain, add ACM + Route53 on top of the ALB.

