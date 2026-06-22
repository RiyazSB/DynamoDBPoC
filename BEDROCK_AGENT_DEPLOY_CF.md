# Deploy Bedrock Agent via CloudFormation

Deploy base resources with CloudFormation, then finish action group + alias in Bedrock Console.

---

## ✅ Working Configuration (Validated)

| Setting | Value |
|---------|-------|
| Foundation Model | `amazon.nova-micro-v1:0` (No access gating) |
| Action Group Executor | Lambda (ZipFile inline code) |
| API Schema Source | S3 (`riyaz-dynamodbpoc-artifacts/bedrock/api-schema.json`) |
| Agent instructions | Strict JSON output rules at top |

---

## Prerequisites

- AWS CLI installed and configured with appropriate credentials
- Your API Gateway endpoint URL (from API Gateway → Stages → prod → Invoke URL)
- Region: **us-east-1**

---

## Step 1: Deploy CloudFormation Stack

```powershell
# PowerShell (Windows)
$API_ENDPOINT="https://5mjiq66mr6.execute-api.us-east-1.amazonaws.com/prod"
$STACK_NAME="bedrock-agent-stack"

aws cloudformation create-stack `
  --stack-name $STACK_NAME `
  --template-body file://infra/cloudformation/bedrock-agent.yml `
  --parameters ParameterKey=APIGatewayEndpoint,ParameterValue=$API_ENDPOINT `
  --region us-east-1 `
  --capabilities CAPABILITY_NAMED_IAM
```

```bash
# Bash (Linux/Mac/WSL)
export API_ENDPOINT="https://5mjiq66mr6.execute-api.us-east-1.amazonaws.com/prod"
aws cloudformation create-stack \
  --stack-name bedrock-agent-stack \
  --template-body file://infra/cloudformation/bedrock-agent.yml \
  --parameters ParameterKey=APIGatewayEndpoint,ParameterValue=$API_ENDPOINT \
  --region us-east-1 \
  --capabilities CAPABILITY_NAMED_IAM
```

---

## Step 2: Upload API Schema to S3

```bash
aws s3 cp infra/bedrock/api-schema.json s3://riyaz-dynamodbpoc-artifacts/bedrock/api-schema.json
```

---

## Step 3: Add Action Group (Bedrock Console)

1. **Bedrock → Agents → `DynamoDBRecordsAgent` → Edit in Agent Builder**
2. Change **Foundation model** to **`Amazon Nova Micro 1.0`** (required — Anthropic models need use-case approval)
3. **Action groups → Add action group**
   - Name: `RecordActions`
   - Executor Lambda: select the Lambda created by the stack
   - API schema: paste content of `infra/bedrock/api-schema.json` OR choose S3 path
4. **Save → Prepare**
5. **Aliases → edit `prod` → point to latest prepared version**

---

## Step 4: Update Lambda Code (Bedrock Response Format)

In Lambda console, replace **all code** in `index.py` with:

```python
import json
import os
from urllib.parse import urlencode
import urllib3

http = urllib3.PoolManager()

def _extract_json_body(request_body):
    if not request_body:
        return None
    if isinstance(request_body, dict) and "content" not in request_body:
        return request_body
    content = request_body.get("content", {}) if isinstance(request_body, dict) else {}
    app_json = content.get("application/json", {})
    props = app_json.get("properties", [])
    if isinstance(props, list):
        out = {}
        for p in props:
            name = p.get("name")
            value = p.get("value")
            if name is not None:
                out[name] = value
        return out if out else None
    return None

def _extract_query_params(parameters):
    if not parameters:
        return {}
    query = {}
    for p in parameters:
        name = p.get("name")
        value = p.get("value")
        if name is not None and value is not None:
            query[name] = value
    return query

def _substitute_path_params(api_path, parameters):
    """Replace {paramName} placeholders in path with actual values."""
    if not parameters or not api_path:
        return api_path, []
    remaining = []
    for p in parameters:
        name = p.get("name")
        value = p.get("value")
        placeholder = "{" + name + "}"
        if placeholder in api_path:
            api_path = api_path.replace(placeholder, str(value))
        else:
            remaining.append(p)
    return api_path, remaining

def lambda_handler(event, context):
    try:
        api_endpoint = os.environ["API_ENDPOINT"].rstrip("/")
        action_group = event.get("actionGroup", "")
        api_path = event.get("apiPath", "")
        http_method = (event.get("httpMethod", "GET") or "GET").upper()
        query_params = _extract_query_params(event.get("parameters", []))
        body_obj = _extract_json_body(event.get("requestBody"))
        url = f"{api_endpoint}{api_path}"
        if query_params:
            url = f"{url}?{urlencode(query_params)}"
        headers = {"Content-Type": "application/json"}
        if http_method in ("POST", "PUT", "PATCH"):
            body_bytes = json.dumps(body_obj or {}).encode("utf-8")
            resp = http.request(http_method, url, body=body_bytes, headers=headers)
        else:
            resp = http.request(http_method, url, headers=headers)
        response_body = resp.data.decode("utf-8") if resp.data else "{}"
        return {
            "messageVersion": "1.0",
            "response": {
                "actionGroup": action_group,
                "apiPath": api_path,
                "httpMethod": http_method,
                "httpStatusCode": int(resp.status),
                "responseBody": {
                    "application/json": {
                        "body": response_body
                    }
                }
            }
        }
    except Exception as e:
        error_body = json.dumps({"error": str(e)})
        return {
            "messageVersion": "1.0",
            "response": {
                "actionGroup": event.get("actionGroup", ""),
                "apiPath": event.get("apiPath", ""),
                "httpMethod": event.get("httpMethod", "GET"),
                "httpStatusCode": 500,
                "responseBody": {
                    "application/json": {
                        "body": error_body
                    }
                }
            }
        }
```

Click **Deploy** after pasting.

---

## Step 5: Agent Instructions (Required for JSON Output)

In Agent Builder → Instructions, add at the **very top**:

```text
Output contract (highest priority):
- For list records, output must be ONLY the raw JSON array from tool response.
- No summary, no markdown, no prefix/suffix text.
- If empty, output exactly [].
- For get record by ID, output the raw JSON object only.
- For create/update, output the raw JSON response from the tool.
- These rules override all other style instructions.
```

---

## Test Prompts (Validated Working)

```
List all records and print exact JSON only
```
```
Create a new record with data "Production bug fix"
```
```
Get the record with ID <paste-id>
```
```
Update record <id> with new data "Updated via agent"
```
```
Delete record <id>
```

---

## IAM Permissions Required on Agent Role

| Action | Resource |
|--------|----------|
| `bedrock:InvokeModel` | `arn:aws:bedrock:us-east-1::foundation-model/*` |
| `bedrock:InvokeModelWithResponseStream` | `arn:aws:bedrock:us-east-1::foundation-model/*` |
| `execute-api:Invoke` | `arn:aws:execute-api:us-east-1:*:*/*` |
| `s3:GetObject` | S3 schema object |
| `logs:*` | CloudWatch logs |

---

## Troubleshooting

| Error | Solution |
|-------|----------|
| `Access denied when calling Bedrock` | Add `bedrock:InvokeModel` to agent IAM role |
| `ARN not found` | Anthropic model requires use-case approval — use `Amazon Nova Micro 1.0` |
| `Lambda Unhandled SyntaxError` | Replace Lambda code with validated version in Step 4 |
| `Lambda response error` | Lambda must return `messageVersion: 1.0` + nested `response` object |
| `Records return []` | Check Lambda `API_ENDPOINT` env var points to correct API Gateway URL |
| `Agent says records exist but shows none` | Add strict JSON output rule to top of agent instructions |

---

## CloudFormation Stack Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `APIGatewayEndpoint` | Required | Your REST API endpoint URL |
| `AgentName` | `DynamoDBRecordsAgent` | Name of Bedrock Agent |
| `FoundationModel` | `amazon.nova-micro-v1:0` | Amazon Nova Micro (no access gating) |
