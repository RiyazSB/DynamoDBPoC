# Add API Gateway + Request Logging to DynamoDBPOC

This CloudFormation template adds AWS API Gateway in front of your existing EC2 app with full request/response logging to CloudWatch.

## What gets created

- **API Gateway** (REST API) — public HTTPS endpoint
- **CloudWatch Logs** — captures all request/response details  
- **IAM roles** — permissions for API Gateway to write logs

## Manual deployment (no CLI)

### Step 1: Get your EC2 instance's public IP

1. Open **EC2 Console** → **Instances**
2. Select your running instance (`dynamodbpoc-ec2`)
3. Copy the **Public IPv4 address** (e.g., `54.123.45.67`)

### Step 2: Create API Gateway stack

1. Go to **CloudFormation** console
2. Click **Create stack** → **With new resources**
3. Upload template: `infra/cloudformation/api-gateway-simple.yml`
4. Click **Next**

### Step 3: Fill parameters

| Parameter | Value |
|---|---|
| `EC2PublicIP` | Paste EC2 public IP from Step 1 (e.g., `54.123.45.67`) |
| `ApiName` | `dynamodbpoc-api` (default is fine) |

Click **Next** → **Next** → check ✅ **I acknowledge that AWS CloudFormation might create IAM resources** → **Submit**.

### Step 4: Wait for stack to complete

Status should become `CREATE_COMPLETE` (~2-3 minutes).

### Step 5: Get your API endpoints

1. Open the stack → **Outputs** tab
2. Copy:
   - **ApiEndpoint** — Main API URL (e.g., `https://xxxxxxxx.execute-api.us-east-1.amazonaws.com/prod`)
   - **SwaggerUrl** — Swagger UI
   - **CloudWatchLogGroup** — Log group name

### Step 6: Test the API

```powershell
$api = "https://xxxxxxxx.execute-api.us-east-1.amazonaws.com/prod"
$body = '{"name":"Via API Gateway","description":"test"}'
Invoke-RestMethod -Method Post -Uri "$api/api/records" -ContentType "application/json" -Body $body
Invoke-RestMethod -Method Get -Uri "$api/api/records"
```

## View incoming request logs

### In CloudWatch Logs (UI)

1. Go to **CloudWatch** → **Logs** → **Log groups**
2. Select `/aws/apigateway/dynamodbpoc-api`
3. Click **Log streams** → select a stream
4. Browse requests (newest first)

Each log shows:
- Timestamp
- HTTP method (POST, GET, etc.)
- Request path
- Status code (200, 404, etc.)
- Latency (milliseconds)
- Request/response body

### Query logs in CloudWatch Insights

1. Go to **CloudWatch** → **Logs Insights**
2. Select log group: `/aws/apigateway/dynamodbpoc-api`
3. Paste a query below and click **Run query**

## Useful CloudWatch Insights queries

**All requests:**
```
fields @timestamp, @message
| sort @timestamp desc
| limit 50
```

**Count requests by method:**
```
fields httpMethod | stats count() by httpMethod
```

**Errors only (non-2xx):**
```
fields @timestamp, status, httpMethod, path
| filter status >= 400
```

**Top slow requests:**
```
fields @timestamp, latency, httpMethod, path
| sort latency desc
| limit 20
```

**Requests to /api/records endpoint:**
```
fields @timestamp, httpMethod, latency, status
| filter path like /api/records/
```

**Requests per hour:**
```
fields @timestamp, httpMethod
| stats count() by bin(1h)
```

## Troubleshooting

### API returns 502 Bad Gateway

**Cause:** EC2 instance is down or port 8080 not responding.

**Fix:**
1. Go to **EC2** → select instance
2. Check instance is `running`
3. Check **Security Group** allows inbound on port 8080
4. Wait ~5 minutes after request to see logs appear (API Gateway takes time to initialize)

### Logs not appearing in CloudWatch

**Cause:** API Gateway is still initializing logging, or no requests succeeded yet.

**Fix:** Wait 1-2 minutes after first request, then refresh CloudWatch Logs console.

## Next steps

- **Monitor performance**: Use CloudWatch Insights queries above to track latency/errors
- **Set alarms**: CloudWatch → Alarms → create alarm on error rate or latency
- **Custom domain**: Route 53 + ACM certificate for production HTTPS URL
- **WAF rules**: API Gateway → Web ACF to add request filtering

## Cleanup

To delete API Gateway (keeps EC2):

1. Go to **CloudFormation**
2. Select stack
3. Click **Delete** → confirm

This removes the API Gateway, but your EC2 app continues running.

