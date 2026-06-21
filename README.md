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
- `spring.cache.type` - cache provider (`redis`)
- `spring.data.redis.host` / `spring.data.redis.port` - Redis endpoint used for API caching

The table uses partition key `id` (String).

## Redis Caching for GET APIs

Redis caching is enabled for:

- `GET /api/records/{id}` (cache name: `recordsById`)
- `GET /api/records` (cache name: `recordsAll`)

Cache invalidation is automatic on create, update, and delete operations.

To run locally, start Redis first (Docker example):

```powershell
docker run --name dynamodbpoc-redis -p 6379:6379 -d redis:7
```

If Redis runs on a different host/port:

```powershell
$env:REDIS_HOST = "<redis-host>"
$env:REDIS_PORT = "6379"
.\mvnw.cmd spring-boot:run
```

For AWS EC2 deployment via CloudFormation (`infra/cloudformation/free-tier-ec2-api-gateway.yml`), you can enable Redis on the same EC2 by setting stack parameter:

- `CacheType=redis`

Optional parameters:

- `RedisHost=127.0.0.1`
- `RedisPort=6379`

### CloudFormation Update Checklist (Redis mode)

Use this when updating your existing stack in AWS Console:

1. Open CloudFormation -> your stack -> **Update**
2. Select **Replace current template** and upload `infra/cloudformation/free-tier-ec2-api-gateway.yml`
3. Keep existing values for app/table/network parameters and set:
   - `CacheType=redis`
   - `RedisHost=127.0.0.1`
   - `RedisPort=6379`
4. Submit the update and wait for `UPDATE_COMPLETE`
5. Verify on EC2 (Session Manager):

```bash
sudo systemctl status redis6 --no-pager || sudo systemctl status redis --no-pager
redis-cli -h 127.0.0.1 -p 6379 ping
sudo systemctl show dynamodbpoc --property=Environment
sudo systemctl status dynamodbpoc --no-pager
```

Expected:

- Redis service is active
- `redis-cli ... ping` returns `PONG`
- App environment includes `SPRING_CACHE_TYPE=redis`

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

## Deploy Latest JAR to EC2

After making code changes, use this flow to push and run the latest build on your EC2 instance.

1. Build latest JAR locally:

```powershell
Set-Location "C:\Users\riyazsb\Downloads\DynamoDBPOC\DynamoDBPOC"
.\mvnw.cmd clean package -DskipTests
```

2. Upload `target\DynamoDBPOC-0.0.1-SNAPSHOT.jar` to S3 bucket `riyaz-dynamodbpoc-artifacts` with object key `DynamoDBPOC-0.0.1-SNAPSHOT.jar`.

3. Connect to EC2 using Session Manager and run:

```bash
sudo aws s3 cp s3://riyaz-dynamodbpoc-artifacts/DynamoDBPOC-0.0.1-SNAPSHOT.jar /opt/dynamodbpoc/app.jar
sudo systemctl restart dynamodbpoc
sudo systemctl status dynamodbpoc --no-pager
```

4. Tail logs to verify requests/changes are active:

```bash
sudo journalctl -u dynamodbpoc -f
```

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


