# AWS Credentials Setup Guide

This guide explains how to configure AWS credentials for multiple accounts and use them with DynamoDB POC.

## Windows Credentials File Location

Your AWS credentials file is located at:
```
C:\Users\<YourUsername>\.aws\credentials
```

## Setting Up Multiple AWS Accounts

Edit your credentials file using Notepad or any text editor:

```ini
[freetier]
aws_access_key_id = YOUR_FREE_TIER_ACCESS_KEY
aws_secret_access_key = YOUR_FREE_TIER_SECRET_KEY

[production]
aws_access_key_id = YOUR_PROD_ACCESS_KEY
aws_secret_access_key = YOUR_PROD_SECRET_KEY

[default]
aws_access_key_id = YOUR_DEFAULT_ACCESS_KEY
aws_secret_access_key = YOUR_DEFAULT_SECRET_KEY
```

**Important:** 
- Replace `YOUR_FREE_TIER_ACCESS_KEY` and `YOUR_FREE_TIER_SECRET_KEY` with your actual credentials
- Do NOT commit this file to version control
- Keep file permissions secure (read-only for your user)

## Obtaining AWS Credentials

1. Log in to your AWS Console
2. Navigate to **IAM** → **Users** → Select your user
3. Click **Security credentials** tab
4. Under **Access keys**, click **Create access key**
5. Choose **Other** as use case
6. Save the **Access Key ID** and **Secret Access Key**

## Configuring DynamoDB POC

### Option 1: Use Free Tier Account (Recommended)

Edit `src/main/resources/application.properties`:

```properties
aws.dynamodb.profile=freetier
aws.dynamodb.region=us-east-1
aws.dynamodb.table-name=records
aws.dynamodb.auto-create-table=false
```

### Option 2: Use Default Account

Leave profile empty (uses default credentials provider chain):

```properties
aws.dynamodb.profile=
aws.dynamodb.region=us-east-1
aws.dynamodb.table-name=records
aws.dynamodb.auto-create-table=false
```

### Option 3: Use Specific Named Profile

```properties
aws.dynamodb.profile=production
aws.dynamodb.region=us-east-1
aws.dynamodb.table-name=records
aws.dynamodb.auto-create-table=false
```

## Region Configuration

Update `aws.dynamodb.region` to match your DynamoDB table region:
- `us-east-1` (N. Virginia)
- `us-west-2` (Oregon)
- `eu-west-1` (Ireland)
- `ap-southeast-1` (Singapore)
- etc.

## Verify Configuration

To test your setup before running the application:

```powershell
# Check credentials file exists and is readable
Test-Path "$env:USERPROFILE\.aws\credentials"

# View AWS profiles configured (content will be masked)
Get-Content "$env:USERPROFILE\.aws\credentials"
```

## Running with Your AWS Account

Once credentials are configured:

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"
.\mvnw.cmd spring-boot:run
```

The application will:
1. Load the specified profile from your credentials file
2. Connect to DynamoDB in your specified region
3. Use the table name configured in properties

## Test Your Connection

After the app starts, test the API with your real DynamoDB:

```powershell
# Create a record
$body = '{"name":"Test Record","description":"Testing real DynamoDB"}'
$response = Invoke-RestMethod -Method Post `
  -Uri 'http://localhost:8080/api/records' `
  -ContentType 'application/json' `
  -Body $body

# Print the ID for retrieval
Write-Host "Created record ID: $($response.id)"

# Get the record back
Invoke-RestMethod -Method Get `
  -Uri "http://localhost:8080/api/records/$($response.id)" `
  -ContentType 'application/json'
```

## Troubleshooting

### "Unable to load credentials" error
- Verify credentials file exists at `$env:USERPROFILE\.aws\credentials`
- Check the profile name matches exactly in application.properties
- Ensure credentials are valid (not expired)

### "AccessDenied" error
- Your IAM user lacks DynamoDB permissions
- Add `AmazonDynamoDBFullAccess` policy to your IAM user (or more restrictive policy)
- Wait a few minutes for IAM changes to propagate

### Table not found
- Verify table exists in your AWS account
- Correct region is set in properties
- Check table name matches exactly

## Security Best Practices

✓ Never commit credentials to version control  
✓ Rotate access keys periodically  
✓ Use least-privilege IAM policies  
✓ Enable MFA on AWS console  
✓ Monitor CloudTrail for unauthorized access  

