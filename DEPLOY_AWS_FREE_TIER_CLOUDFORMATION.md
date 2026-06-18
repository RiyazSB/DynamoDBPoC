# Deploy to AWS Free Tier with CloudFormation (EC2 + API Gateway)

This deployment path is designed for AWS Free Tier usage:

- Single EC2 instance (`t3.micro` default) running your Spring Boot jar
- IAM role on EC2 for DynamoDB access (no static keys on server)
- API Gateway HTTP API proxy in the same stack
- Uses your existing S3 bucket for artifact delivery

## Files

- Single-stack template (recommended): `infra/cloudformation/free-tier-ec2-api-gateway.yml`
- EC2-only template: `infra/cloudformation/free-tier-simple.yml`
- One-click script: `deploy-free-tier.ps1`

## Your current defaults

- Artifact bucket: `riyaz-dynamodbpoc-artifacts`
- JAR object key: `DynamoDBPOC-0.0.1-SNAPSHOT.jar`
- DynamoDB table: `records`

## DynamoDB table creation options

- If table `records` already exists: set `CreateDynamoTable=false` (recommended for your current setup).
- If you want CloudFormation to create/manage the table: set `CreateDynamoTable=true`.

The template creates a simple on-demand table with partition key `id` (String).

## API request logging (no Lambda required)

The single-stack template enables API Gateway HTTP API access logging to CloudWatch.

- Parameter: `ApiAccessLogRetentionDays` (default `7`)
- Output: `ApiAccessLogGroupName`

You can view logs in **CloudWatch -> Logs -> Log groups** using the output name.
Each request includes method, path, status, source IP, integration status, and latency.

## Quick deploy (recommended)

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"
.\deploy-free-tier.ps1
```

This script will:

1. Validate AWS profile access (`dynamo_local_dev`)
2. Detect default VPC/subnet
3. Build JAR
4. Deploy CloudFormation stack
5. Upload JAR to `s3://riyaz-dynamodbpoc-artifacts/DynamoDBPOC-0.0.1-SNAPSHOT.jar`
6. Print API and Swagger URLs

## Manual CloudFormation console deploy (no CLI path)

1. Build JAR:

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"
.\mvnw.cmd clean package -DskipTests
```

2. Upload `target\DynamoDBPOC-0.0.1-SNAPSHOT.jar` to S3 bucket `riyaz-dynamodbpoc-artifacts`
3. Open CloudFormation in AWS Console and create stack with template `infra/cloudformation/free-tier-ec2-api-gateway.yml`
4. Parameters:
   - `ArtifactBucketName=riyaz-dynamodbpoc-artifacts`
   - `JarObjectKey=DynamoDBPOC-0.0.1-SNAPSHOT.jar`
   - `InstanceType=t3.micro` (if `t2.micro` is not eligible in your account/region)
   - `DynamoTableName=records`
   - `CreateDynamoTable=false` (or `true` for new table creation)
   - `ApiAccessLogRetentionDays=7`
   - Choose your `VpcId` and `SubnetId`
5. After stack completes, use outputs:
   - `ApiGatewayEndpoint`
   - `ApiGatewayRecordsUrl`
   - `ApiGatewaySwaggerUrl`
   - `DirectApiBaseUrl`
   - `DirectSwaggerUrl`

## Cleanup

```powershell
aws cloudformation delete-stack --stack-name dynamodbpoc-free-tier --region us-east-1 --profile dynamo_local_dev
```
