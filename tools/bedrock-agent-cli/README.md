# Bedrock Agent CLI (VS Code)

Use this script to run your Bedrock Agent directly from VS Code terminal.

## 1) Prerequisites

- Python 3.10+
- AWS credentials configured (profile or environment)
- IAM permissions for:
  - `bedrock:InvokeAgent`
  - `bedrock:GetAgent`
  - `bedrock:GetAgentAlias`
  - `bedrock:ListAgentAliases`

## 2) Setup

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC\tools\bedrock-agent-cli"
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
```

## 3) Run (single prompt)

```powershell
python .\run_agent.py --agent-id CLSYHNOYIW --alias-name prod --region us-east-1 --profile default --prompt "List all records and print exact JSON only"
```

## 4) Run (interactive chat)

```powershell
python .\run_agent.py --agent-id CLSYHNOYIW --alias-name prod --region us-east-1 --profile default
```

Then type prompts like:
- `List all records and print exact JSON only`
- `Create a record with name Testing from VS Code and description test`
- `Update record <id> with name Updated from VS Code`
- `Delete record <id>`

## 5) If alias lookup fails

Use explicit alias ID:

```powershell
python .\run_agent.py --agent-id CLSYHNOYIW --alias-id PEAAX0JY4B --region us-east-1 --profile default
```

## 6) Common errors

- `Access denied when calling Bedrock`:
  - Check IAM permission on the identity used in VS Code terminal.
- `Could not find alias 'prod'`:
  - Alias name differs; pass `--alias-id` directly.
- Empty output:
  - Add stricter prompt: `List all records and print exact JSON only`.

