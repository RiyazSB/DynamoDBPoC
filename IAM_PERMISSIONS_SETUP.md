# DynamoDB IAM Permissions Setup

Your app is using the IAM user: **`elastic-search-demo`**

This user currently lacks DynamoDB permissions. You have two options:

## Option 1: Grant DynamoDB Permissions (Easiest)

### Steps:

1. Open [AWS IAM Console](https://console.aws.amazon.com/iam/)
2. Click **Users** in the left sidebar
3. Find and click on **`elastic-search-demo`**
4. Click the **Permissions** tab
5. Click **Add permissions** button → **Attach policies directly**
6. In the search box, type: `AmazonDynamoDBFullAccess`
7. Check the box next to it
8. Click **Add permissions**
9. Wait ~30 seconds for the permission to take effect
10. Go back to your terminal and run the app again:

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"
.\mvnw.cmd spring-boot:run
```

The table should now be created automatically!

## Option 2: Create Table Manually (If You Prefer Limited Permissions)

If you want more granular control, create the table manually:

### Steps:

1. Open [AWS DynamoDB Console](https://console.aws.amazon.com/dynamodb/)
2. Click **Tables** on the left
3. Click **Create table**
4. Fill in:
   - **Table name:** `records`
   - **Partition key:** `id` (String)
   - **Billing mode:** Pay per request (on-demand)
5. Click **Create table**
6. Wait for table status to show "Active"
7. Run your app:

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"
.\mvnw.cmd spring-boot:run
```

Your app will now connect to the existing table!

## Option 3: Use a Different IAM User

If you have another IAM user with DynamoDB permissions:

1. Add that user's credentials to `~/.aws/credentials`:
   ```ini
   [freetier]
   aws_access_key_id = NEW_KEY_ID
   aws_secret_access_key = NEW_SECRET_KEY
   ```

2. Update `application.properties`:
   ```properties
   aws.dynamodb.profile=freetier
   aws.dynamodb.auto-create-table=true
   ```

3. Run the app

## Verify Permissions

To check what permissions your user has:

1. Go to [IAM Users Console](https://console.aws.amazon.com/iam/home#/users)
2. Click on `elastic-search-demo`
3. Check **Permissions** tab - should see `AmazonDynamoDBFullAccess` or similar

## Recommended Policy (If You Want Read-Only or Limited Access)

For production, instead of full access, use this minimal policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "dynamodb:PutItem",
        "dynamodb:GetItem",
        "dynamodb:UpdateItem",
        "dynamodb:DeleteItem",
        "dynamodb:Scan",
        "dynamodb:Query"
      ],
      "Resource": "arn:aws:dynamodb:us-east-1:610269527763:table/records"
    }
  ]
}
```

**To use this policy:**
1. Go to [IAM Policies Console](https://console.aws.amazon.com/iam/home#/policies)
2. Click **Create policy**
3. Go to **JSON** tab and paste the policy above
4. Click **Next**
5. Name it: `DynamoDBAppAccess`
6. Click **Create policy**
7. Go to Users → `elastic-search-demo` → **Add permissions** → **Attach policies directly**
8. Search for `DynamoDBAppAccess` and attach it

Then your user will only have access to this specific table!

