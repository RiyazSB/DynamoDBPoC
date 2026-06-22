# Amazon Bedrock Agent Setup Guide

## Overview
This guide helps you create a Bedrock Agent that can understand natural language and call your DynamoDB Records REST API endpoints.

**Agent Invocation Example:**
```
User: "Create a record with data 'Production bug in auth service'"
→ Agent calls: POST /api/records with {"data": "Production bug in auth service"}
→ Agent returns: Record created with ID, timestamps

User: "List all records"
→ Agent calls: GET /api/records
→ Agent returns: Array of all records

User: "Update record abc-123 with new data 'Fixed the auth bug'"
→ Agent calls: PUT /api/records/abc-123 with {"data": "Fixed the auth bug"}
→ Agent returns: Updated record

User: "Delete record xyz-789"
→ Agent calls: DELETE /api/records/xyz-789
→ Agent returns: Deletion confirmed
```

---

## Prerequisites

- AWS Account with appropriate permissions
- Your API Gateway endpoint (e.g., `https://5mjiq66mr6.execute-api.us-east-1.amazonaws.com/prod`)
- OpenAPI schema: `infra/bedrock/api-schema.json` (already created)

---

## Step-by-Step Setup

### **Step 1: Get Your API Gateway URL**

1. Go to **AWS Console** → **API Gateway**
2. Find your API (should be named something like `DynamoDBPOCApi`)
3. Click **Stages** → **prod**
4. Copy the **Invoke URL** (should look like `https://xxxxx.execute-api.us-east-1.amazonaws.com/prod`)

---

### **Step 2: Create IAM Role for Bedrock Agent**

1. Go to **AWS Console** → **IAM** → **Roles**
2. Click **Create Role**
3. Select **AWS Service** → **Bedrock** → **Bedrock Agent**
4. Click **Next**
5. Click **Create Policy** (new tab opens)
6. Choose **JSON** editor and paste:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Effect": "Allow",
            "Action": [
                "execute-api:Invoke"
            ],
            "Resource": "arn:aws:execute-api:us-east-1:*:*/*"
        },
        {
            "Effect": "Allow",
            "Action": [
                "logs:CreateLogGroup",
                "logs:CreateLogStream",
                "logs:PutLogEvents"
            ],
            "Resource": "arn:aws:logs:*:*:*"
        }
    ]
}
```

7. Click **Next** → Give it a name like `BedrockAgentAPIPolicy` → **Create Policy**
8. Back to role creation tab, refresh policies, select the one you just created
9. Click **Next** → Name the role `BedrockAgentAPIRole`
10. Click **Create Role**

---

### **Step 3: Create the Bedrock Agent**

1. Go to **AWS Console** → **Bedrock** (search for it)
2. On the left sidebar, click **Agents** (under **Build**)
3. Click **Create Agent**
4. **Agent Name:** `DynamoDBRecordsAgent`
5. **Agent description:** `Agent for managing DynamoDB records via REST API`
6. **IAM permissions:** Select `BedrockAgentAPIRole` (from Step 2)
7. Click **Create Agent**

---

### **Step 4: Create Action Group**

1. Inside your agent, scroll to **Action groups** section
2. Click **Add action group**
3. **Action group name:** `RecordActions`
4. **Action group description:** `CRUD operations for DynamoDB records`

---

### **Step 5: Add API Schema**

1. In **Action invocation**, select **API**
2. **API type:** REST
3. **API endpoint:** Paste your API Gateway URL (e.g., `https://5mjiq66mr6.execute-api.us-east-1.amazonaws.com/prod`)

4. **API schema** → Select **Define with JSON Editor**
5. Open `infra/bedrock/api-schema.json` from your repo (locally)
6. **Copy the entire JSON content** and paste into the editor
7. Make sure the `servers[0].url` in the schema matches your API Gateway URL:

```json
"servers": [
  {
    "url": "https://5mjiq66mr6.execute-api.us-east-1.amazonaws.com/prod",
    "description": "API Gateway endpoint"
  }
]
```

8. Click **Create action group**

---

### **Step 6: Save and Test Agent**

1. Click **Save and exit**
2. You'll see a **Prepare Agent** button (may take 30-60 seconds)
3. Click **Prepare agent** and wait for it to complete
4. Once ready, click **Test Agent** tab

---

### **Step 7: Test with Natural Language**

In the **Test Agent** pane, try these prompts:

**Test 1: List all records**
```
List all records
```

**Test 2: Create a record**
```
Create a new record with the data "Important task"
```

**Test 3: Get specific record**
```
Get the record with ID abc-123-def-456
```

**Test 4: Update record**
```
Update record abc-123-def-456 with new data "Updated task"
```

**Test 5: Delete record**
```
Delete record abc-123-def-456
```

---

## Expected Behavior

1. **First request** → Agent will ask clarification if needed
2. **Agent processes** → You'll see the thinking process: "Deciding which action to invoke..."
3. **Agent calls your API** → Shows the API call details
4. **Response** → Agent returns the result in natural language

Example output:
```
Agent: I'll create a new record with your data.
[Calling API: POST /api/records]
[Response: 200 OK]
Agent: I've successfully created the record. Here are the details:
- Record ID: 550e8400-e29b-41d4-a716-446655440000
- Data: Important task
- Created: 2024-06-22T10:30:45Z
```

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| **404 errors when agent calls API** | Verify API Gateway URL is correct in schema and accessible from AWS |
| **Access Denied (403)** | Check IAM role has `execute-api:Invoke` permission and is attached to agent |
| **Agent can't prepare** | Wait 2-3 minutes, check CloudWatch logs under IAM role for errors |
| **API timeout** | Ensure EC2/app is running and API Gateway is correctly pointing to it |

---

## Next Steps (Optional)

- **Add API Key authentication** → Add `X-API-Key` header to schema if needed
- **Expose agent in a web UI** → Create Lambda wrapper + API Gateway for public bot access
- **Connect to Slack/Teams** → Use Bedrock Agent integration (native support)
- **Add prompt engineering** → Customize agent instructions for specific use cases


