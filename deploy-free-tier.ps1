<#
.SYNOPSIS
  One-shot deploy of DynamoDBPOC to AWS Free Tier using CloudFormation.

.DESCRIPTION
  - Detects default VPC and subnet automatically
  - Builds the Spring Boot JAR
  - Deploys CloudFormation stack (creates S3 bucket, EC2, IAM role, security group)
  - Uploads JAR to the S3 bucket created by the stack
  - Waits for EC2 to boot and shows API/Swagger URLs

.PARAMETER Region
  AWS region. Default: us-east-1

.PARAMETER DynamoTableName
  Your existing DynamoDB table name. Default: records

.PARAMETER StackName
  CloudFormation stack name. Default: dynamodbpoc-free-tier

.PARAMETER ProjectName
  Project name prefix used for all resources. Default: dynamodbpoc

.EXAMPLE
  .\deploy-free-tier.ps1

.EXAMPLE
  .\deploy-free-tier.ps1 -Region ap-southeast-1 -DynamoTableName my-table
#>

param(
    [string]$Region = "us-east-1",
    [string]$DynamoTableName = "records",
    [string]$StackName = "dynamodbpoc-free-tier",
    [string]$ProjectName = "dynamodbpoc",
    [string]$Profile = "dynamo_local_dev",
    [string]$ArtifactBucketName = "riyaz-dynamodbpoc-artifacts",
    [string]$JarObjectKey = "DynamoDBPOC-0.0.1-SNAPSHOT.jar",
    [string]$InstanceType = "t3.micro"
)

# Build reusable profile arg (empty string = use default)
$ProfileArg = if ($Profile) { @("--profile", $Profile) } else { @() }

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Definition

Write-Host ""
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "  DynamoDBPOC Free Tier Deployer" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""

# ─── Step 1: Verify AWS CLI is installed ─────────────────────────────────────
Write-Host "[1/7] Checking AWS CLI..." -ForegroundColor Yellow
Write-Host "      Using profile : $(if ($Profile) { $Profile } else { '(default)' })" -ForegroundColor Gray
try {
    $identity = & aws sts get-caller-identity --region $Region @ProfileArg --output json 2>&1 | ConvertFrom-Json
    Write-Host "      AWS Account : $($identity.Account)" -ForegroundColor Green
    Write-Host "      IAM User    : $($identity.Arn)" -ForegroundColor Green
} catch {
    Write-Host "ERROR: AWS CLI not found or not configured." -ForegroundColor Red
    Write-Host "Run: aws configure --profile dynamo_local_dev" -ForegroundColor Red
    exit 1
}

# ─── Step 2: Detect default VPC and public subnet ─────────────────────────────
Write-Host ""
Write-Host "[2/7] Detecting default VPC and subnet..." -ForegroundColor Yellow

$VpcId = & aws ec2 describe-vpcs `
    --filters "Name=isDefault,Values=true" `
    --query "Vpcs[0].VpcId" `
    --output text `
    --region $Region @ProfileArg

if (-not $VpcId -or $VpcId -eq "None") {
    Write-Host "ERROR: No default VPC found in $Region." -ForegroundColor Red
    Write-Host "Create a default VPC with: aws ec2 create-default-vpc --region $Region" -ForegroundColor Red
    exit 1
}

$SubnetId = & aws ec2 describe-subnets `
    --filters "Name=vpc-id,Values=$VpcId" "Name=mapPublicIpOnLaunch,Values=true" `
    --query "Subnets[0].SubnetId" `
    --output text `
    --region $Region @ProfileArg

if (-not $SubnetId -or $SubnetId -eq "None") {
    # Fallback: take first subnet in default VPC
    $SubnetId = & aws ec2 describe-subnets `
        --filters "Name=vpc-id,Values=$VpcId" `
        --query "Subnets[0].SubnetId" `
        --output text `
        --region $Region @ProfileArg
}

Write-Host "      VPC    : $VpcId" -ForegroundColor Green
Write-Host "      Subnet : $SubnetId" -ForegroundColor Green

# ─── Step 3: Build JAR ────────────────────────────────────────────────────────
Write-Host ""
Write-Host "[3/7] Building Spring Boot JAR..." -ForegroundColor Yellow

Push-Location $ScriptDir
try {
    & .\mvnw.cmd clean package -DskipTests -q
    if ($LASTEXITCODE -ne 0) { throw "Maven build failed" }
} finally {
    Pop-Location
}

$JarPath = Get-ChildItem "$ScriptDir\target\*.jar" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $JarPath) {
    Write-Host "ERROR: JAR not found in target/ after build." -ForegroundColor Red
    exit 1
}
Write-Host "      JAR : $($JarPath.Name)" -ForegroundColor Green

# ─── Step 4: Deploy CloudFormation stack ─────────────────────────────────────
Write-Host ""
Write-Host "[4/7] Deploying CloudFormation stack '$StackName'..." -ForegroundColor Yellow
Write-Host "      (This creates EC2, IAM role, Security Group)" -ForegroundColor Gray

& aws cloudformation deploy `
    --stack-name $StackName `
    --template-file "$ScriptDir\infra\cloudformation\free-tier-simple.yml" `
    --parameter-overrides `
        "ProjectName=$ProjectName" `
        "VpcId=$VpcId" `
        "SubnetId=$SubnetId" `
        "DynamoTableName=$DynamoTableName" `
        "ArtifactBucketName=$ArtifactBucketName" `
        "JarObjectKey=$JarObjectKey" `
        "InstanceType=$InstanceType" `
    --capabilities CAPABILITY_NAMED_IAM `
    --region $Region @ProfileArg

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: CloudFormation deploy failed. Check AWS Console > CloudFormation > Events for details." -ForegroundColor Red
    exit 1
}

# ─── Step 5: Get stack outputs ────────────────────────────────────────────────
Write-Host ""
Write-Host "[5/7] Fetching stack outputs..." -ForegroundColor Yellow

$outputs = & aws cloudformation describe-stacks `
    --stack-name $StackName `
    --query "Stacks[0].Outputs" `
    --output json `
    --region $Region @ProfileArg | ConvertFrom-Json

$outputMap = @{}
foreach ($o in $outputs) { $outputMap[$o.OutputKey] = $o.OutputValue }

$S3Bucket     = $outputMap["ArtifactBucketName"]
$S3Key        = $outputMap["JarObjectKey"]
$InstanceId   = $outputMap["InstanceId"]
$ApiBaseUrl   = $outputMap["ApiBaseUrl"]
$SwaggerUrl   = $outputMap["SwaggerUrl"]

Write-Host "      S3 Bucket  : $S3Bucket" -ForegroundColor Green
Write-Host "      S3 Key     : $S3Key" -ForegroundColor Green
Write-Host "      Instance   : $InstanceId" -ForegroundColor Green

# ─── Step 6: Upload JAR to S3 ─────────────────────────────────────────────────
Write-Host ""
Write-Host "[6/7] Uploading JAR to S3 bucket..." -ForegroundColor Yellow

& aws s3 cp $JarPath.FullName "s3://$S3Bucket/$S3Key" --region $Region @ProfileArg

if ($LASTEXITCODE -ne 0) {
    Write-Host "ERROR: Failed to upload JAR to S3." -ForegroundColor Red
    exit 1
}
Write-Host "      Uploaded to s3://$S3Bucket/$S3Key" -ForegroundColor Green

# ─── Step 7: Wait for EC2 to be ready ────────────────────────────────────────
Write-Host ""
Write-Host "[7/7] Waiting for EC2 instance to be healthy..." -ForegroundColor Yellow
Write-Host "      (This takes about 2-3 minutes while Java installs and app starts)" -ForegroundColor Gray

& aws ec2 wait instance-running --instance-ids $InstanceId --region $Region @ProfileArg
Write-Host "      EC2 is running. App may need 1-2 more minutes to start." -ForegroundColor Green

# ─── Done ──────────────────────────────────────────────────────────────────────
Write-Host ""
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "  Deployment Complete!" -ForegroundColor Green
Write-Host "===========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "  API URL     : $ApiBaseUrl" -ForegroundColor White
Write-Host "  Swagger UI  : $SwaggerUrl" -ForegroundColor White
Write-Host ""
Write-Host "  Wait ~2 minutes then test:" -ForegroundColor Yellow
Write-Host ""
Write-Host "  `$body = '{\"name\":\"Hello\",\"description\":\"from AWS\"}'" -ForegroundColor Gray
Write-Host "  Invoke-RestMethod -Method Post -Uri '$ApiBaseUrl/api/records' -ContentType 'application/json' -Body `$body" -ForegroundColor Gray
Write-Host ""
Write-Host "  To redeploy after code change:" -ForegroundColor Yellow
Write-Host "  .\deploy-free-tier.ps1" -ForegroundColor Gray
Write-Host ""
Write-Host "  To delete all resources:" -ForegroundColor Yellow
Write-Host "  aws cloudformation delete-stack --stack-name $StackName --region $Region --profile $Profile" -ForegroundColor Gray
Write-Host ""

