# CloudFormation Cleanup Scheduler

This stack creates an EventBridge schedule + Lambda that deletes selected CloudFormation stacks at a fixed UTC time.

## Files

- `infra/cloudformation/stack-cleanup-scheduler.yml`
- `infra/cloudformation/stack-cleanup-parameters-example.json`

## Safety defaults

- `DryRun=true` by default (logs only, does not delete).
- Scheduler stack never deletes itself.
- Deletion runs only if at least one selector is set:
  - `StackPrefix`, or
  - `StackNamesCsv`

## Deploy (Windows PowerShell)

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"

aws cloudformation create-stack `
  --stack-name cfn-cleanup-scheduler `
  --template-body file://infra/cloudformation/stack-cleanup-scheduler.yml `
  --parameters file://infra/cloudformation/stack-cleanup-parameters-example.json `
  --capabilities CAPABILITY_NAMED_IAM `
  --region us-east-1
```

## Test in dry-run mode first

1. Keep `DryRun=true` in parameters.
2. Wait for stack create complete.
3. Invoke Lambda manually from Console (Test) and check CloudWatch logs.

## Enable real deletion

Update parameters and set:

- `DryRun=false`

Then run stack update:

```powershell
aws cloudformation update-stack `
  --stack-name cfn-cleanup-scheduler `
  --template-body file://infra/cloudformation/stack-cleanup-scheduler.yml `
  --parameters file://infra/cloudformation/stack-cleanup-parameters-example.json `
  --capabilities CAPABILITY_NAMED_IAM `
  --region us-east-1
```

## Parameter guidance

- `ScheduleExpressionUtc`: EventBridge cron in **UTC**.
- `StackPrefix`: delete all stack names starting with this prefix.
- `StackNamesCsv`: exact stack names (comma-separated).
- `ExcludeStacksCsv`: always skip these stacks.

## Example

If you want daily cleanup at 01:00 PM IST (07:30 UTC), use:

- `ScheduleExpressionUtc = cron(30 7 * * ? *)`

## Important

Stack deletion is irreversible for resources without retention policies. Keep critical stacks in `ExcludeStacksCsv`.
