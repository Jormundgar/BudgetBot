# BudgetBot

A Telegram bot for monitoring the available balance in YNAB (You Need A Budget).  
The application is built with Spring Boot, integrates with the YNAB API and the Telegram Bot API, stores balance state and recipient lists in DynamoDB, sends daily notifications, and responds to commands via a webhook.

## Features

- Fetches the available balance from YNAB (“to be budgeted”) with server knowledge support.
- Telegram command to request the current balance.
- Scheduled daily balance notifications.
- Telegram webhook for user registration and command handling.
- Manually triggered job endpoint to check for balance changes and send notifications.
- Persists the last known balance and server knowledge in DynamoDB.

## Architecture

- **YNAB integration**: `YnabClient` calls `/budgets/{budgetId}/months/{month}` and supports the `last_knowledge_of_server` parameter to reduce traffic.
- **Telegram webhook**: `TelegramWebhookController` receives updates, handles `/start` (recipient registration), and processes the balance request command.
- **Daily scheduler**: `DailyBalanceJob` sends the balance to all recipients via cron.
- **Change detection**: `BalancePollService` compares the current value with the stored one and notifies recipients only if it has changed.
- **DynamoDB**: stores balance state and the list of recipient chat IDs.

## Requirements

- Java 17
- Maven 3.9+
- AWS access (DynamoDB) for the non-dev profile

## Configuration

Core configuration is defined in `src/main/resources/application.yml` and can be overridden via environment variables.

### Environment Variables

| Variable | Description |
| --- | --- |
| `YNAB_TOKEN` | OAuth token for the YNAB API |
| `YNAB_BUDGET_ID` | YNAB budget ID |
| `TELEGRAM_BOT_TOKEN` | Telegram bot token |
| `TELEGRAM_WEBHOOK_SECRET` | Secret for validating webhook requests (`X-Telegram-Bot-Api-Secret-Token`) |
| `BUDGETBOT_JOB_TOKEN` | Token for `/jobs/poll` (`X-Budgetbot-Job-Token`) |
| `DYNAMODB_TABLE` | DynamoDB table for storing balance state |
| `DYNAMODB_RECIPIENTS_TABLE` | DynamoDB table for storing recipients |

### Additional Settings (`application.yml`)

- `server.port` — HTTP server port (default: 8080)
- `jobs.cron` and `jobs.zone` — schedule and time zone for the daily job
- `currency.symbol` and `currency.fraction-digits` — currency formatting

## HTTP Endpoints

### Telegram webhook
```
POST /telegram/webhook
X-Telegram-Bot-Api-Secret-Token: <TELEGRAM_WEBHOOK_SECRET>
```


- `POST /telegram/webhook` — receives Telegram updates
- `/start` registers a user in the recipients table and sends a keyboard
- The **"Get current balance"** button returns the current available balance

### Job endpoint

```
POST /jobs/poll
X-Budgetbot-Job-Token: <BUDGETBOT_JOB_TOKEN>
```

Triggers a balance change check and sends notifications if the value has changed.

### Actuator

- `GET /actuator/health`
- `GET /actuator/info`

## Running Locally

1. Install Java 17.
2. Export the required environment variables.
3. Start the application:

```
./mvnw spring-boot:run
```


## Build and Run with Docker

```
docker build -t budgetbot .
docker run -p 8080:8080 \
  -e YNAB_TOKEN=... \
  -e YNAB_BUDGET_ID=... \
  -e TELEGRAM_BOT_TOKEN=... \
  -e TELEGRAM_WEBHOOK_SECRET=... \
  -e BUDGETBOT_JOB_TOKEN=... \
  -e DYNAMODB_TABLE=... \
  -e DYNAMODB_RECIPIENTS_TABLE=... \
  budgetbot
```


## Deployment

The repository includes a GitHub Actions workflow that builds the Docker image, pushes it to ECR, and updates the ECS service. AWS credentials are provided via OIDC.

## Testing

```
./mvnw test
```
