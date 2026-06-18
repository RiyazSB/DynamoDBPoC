# DynamoDBPOC REST API

Spring Boot CRUD APIs backed by AWS DynamoDB.

## Endpoints

- `POST /api/records` - create record
- `GET /api/records` - list all records
- `GET /api/records/{id}` - fetch record by id
- `PUT /api/records/{id}` - update existing record
- `DELETE /api/records/{id}` - delete record

## API Docs

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Configuration

**First, set up your AWS credentials** → See `AWS_CREDENTIALS_SETUP.md`

Then, configure these properties in `src/main/resources/application.properties`:

- `aws.dynamodb.profile` - AWS credentials profile name (from ~/.aws/credentials)
- `aws.dynamodb.region` - AWS region
- `aws.dynamodb.table-name` - DynamoDB table name
- `aws.dynamodb.endpoint` - optional endpoint (use for DynamoDB Local)
- `aws.dynamodb.auto-create-table` - set `true` to create the table at startup

The table uses partition key `id` (String).

## Connect to Real DynamoDB

To use a real AWS account (e.g., free tier):

1. Add your AWS credentials to `~/.aws/credentials` file
2. Set `aws.dynamodb.profile=freetier` (or your profile name) in `application.properties`
3. Set correct region (e.g., `us-east-1`)
4. Run the application

## Deploy to AWS

For AWS deployment options:

- Free-tier focused EC2 deployment: `DEPLOY_AWS_FREE_TIER_CLOUDFORMATION.md`
- ECS Fargate deployment: `DEPLOY_AWS_CLOUDFORMATION.md`

## Add API Gateway + Logging

To put an API Gateway in front of your EC2 app with CloudWatch request logs:

- `API_GATEWAY_SETUP.md` (setup guide with example log queries)

## Run

```powershell
.\mvnw.cmd spring-boot:run
```

## Quick API test

```powershell
$body = '{"name":"Sample","description":"First record"}'
Invoke-RestMethod -Method Post -Uri 'http://localhost:8080/api/records' -ContentType 'application/json' -Body $body
```


