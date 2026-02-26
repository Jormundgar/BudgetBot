# BudgetBot

BudgetBot is a Spring Boot service that connects a Telegram bot + Telegram Mini App with the YNAB API so users can see their current **To Be Budgeted** amount and recent transactions directly inside Telegram.

The project supports:
- user registration through the Telegram bot (`/start`);
- per-user YNAB OAuth 2.0 authorization;
- personal balance and transactions requests from the Mini App;
- daily scheduled balance messages;
- a protected polling job endpoint that notifies only when balance changes;
- state storage in memory (`dev`) or DynamoDB (`!dev`), with refresh token encryption via AWS KMS.

---

## Table of Contents
- [Tech Stack](#tech-stack)
- [How It Works](#how-it-works)
- [Architecture and Core Components](#architecture-and-core-components)
- [Profiles and Storage](#profiles-and-storage)
- [Configuration](#configuration)
- [API Endpoints](#api-endpoints)
- [Run Locally](#run-locally)
- [Build and Run with Docker](#build-and-run-with-docker)
- [Testing and Quality](#testing-and-quality)

---

## Tech Stack

- **Java 17**
- **Spring Boot 3.2** (Web, Actuator, Scheduling, Test)
- **Maven Wrapper** (`./mvnw`)
- **AWS SDK v2** (DynamoDB, KMS)
- **Telegram Bot API**
- **YNAB API + OAuth 2.0**

---

## How It Works

1. A user sends `/start` to the bot.
2. The bot adds the user to the allowed recipients store and sends a keyboard button to open the Mini App.
3. In the Mini App, the user connects YNAB via OAuth.
4. After connection, the user can:
- view current balance;
- force refresh balance;
- view recent transactions.
5. A scheduled daily job sends the current balance.
6. A separate protected polling endpoint can be triggered externally and sends a message only when balance changes.

---

## Architecture and Core Components

### Telegram entrypoint
- `POST /telegram/webhook` — receives Telegram updates, validates webhook secret, handles `/start`.
- `TelegramClient` — sends messages and the Mini App keyboard button.

### Mini App backend
- `POST /api/miniapp/oauth/start` — starts OAuth flow (returns YNAB authorization URL).
- `GET /api/miniapp/oauth/callback` — exchanges authorization code for tokens and stores user connection.
- `POST /api/miniapp/balance` — returns current balance.
- `POST /api/miniapp/refresh` — forces balance refresh.
- `POST /api/miniapp/transactions` — returns latest transactions.

### Balance business logic
- `BalanceService`:
- refreshes user access token from refresh token;
- picks target month (after day 10 it requests the next month);
- calls YNAB month endpoint;
- formats amount for Telegram.

### Scheduler and polling
- `DailyBalanceJob` — sends daily balance to all connected users.
- `JobsController` (`POST /jobs/poll`) — manual/external trigger for change detection.
- `BalancePollService` — uses `server_knowledge` and last value state to avoid duplicate notifications.

### Data infrastructure
- `RecipientsStore` — allowed Telegram users.
- `UserYnabConnectionStore` — user refresh token + budget ID.
- `BalanceStateStore` — last known balance and `serverKnowledge`.
- In non-`dev`, DynamoDB is used; refresh tokens are encrypted with KMS.

---

## Profiles and Storage

Default Spring profile is `dev`.

### `dev`
- In-memory store implementations.
- Best for local development.
- Data is not persisted across restarts.

### `!dev` (any profile except `dev`)
- DynamoDB-backed store implementations.
- Requires DynamoDB tables and AWS KMS access (for refresh token encryption).

---
# Configuration
Base configuration lives in `src/main/resources/application.yml`. Critical values should be provided via environment variables.
### Required environment variables (production-like setup)
| Variable | Purpose |
|---|---|
 `TELEGRAM_BOT_TOKEN` | Telegram bot token |
| `TELEGRAM_WEBHOOK_SECRET` | Secret for `X-Telegram-Bot-Api-Secret-Token` webhook header |
| `TELEGRAM_MINIAPP_URL` | Public Mini App URL |
| `YNAB_CLIENT_ID` | YNAB OAuth client ID |
| `YNAB_CLIENT_SECRET` | YNAB OAuth client secret |
| `YNAB_REDIRECT_URI` | OAuth callback redirect URI |
| `BUDGETBOT_JOB_TOKEN` | Token for `POST /jobs/poll` |
| `DYNAMODB_RECIPIENTS_TABLE` | Recipients table |
| `DYNAMODB_BALANCE_STATE_TABLE` | Balance state table |
| `DYNAMODB_YNAB_CONNECTIONS_TABLE` | User↔YNAB connections table |
| `KMS_KEY_ID` | KMS key id/arn for refresh token encryption |

### Important `application.yml` keys

| Key | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP server port |
| `jobs.cron` | `0 0 7 * * *` | Daily scheduler cron |
| `jobs.zone` | `Asia/Jerusalem` | Time zone for business logic and scheduler |
| `currency.symbol` | `₪` | Currency symbol |
| `currency.fraction-digits` | `2` | Decimal digits for formatting |

---

## API Endpoints
## Telegram webhook

```http
POST /telegram/webhook
-Telegram-Bot-Api-Secret-Token: <TELEGRAM_WEBHOOK_SECRET>
Content-Type: application/json
```

### Mini App API

```http
POST /api/miniapp/oauth/start
POST /api/miniapp/balance
POST /api/miniapp/refresh
POST /api/miniapp/transactions
GET  /api/miniapp/oauth/callback?code=...&state=...
```

> For Mini App `POST` endpoints, request body should be `{ "initData": "..." }`.

### Jobs endpoint

```http
POST /jobs/poll
X-Budgetbot-Job-Token: <BUDGETBOT_JOB_TOKEN>
```
---

## Run Locally

### 1) Set environment variables
```bash
export TELEGRAM_BOT_TOKEN=...
export TELEGRAM_WEBHOOK_SECRET=...
export TELEGRAM_MINIAPP_URL=...
export YNAB_CLIENT_ID=...
export YNAB_CLIENT_SECRET=...
export YNAB_REDIRECT_URI=...

export BUDGETBOT_JOB_TOKEN=...
```

For `dev` profile, DynamoDB/KMS settings are not required.

### 2) Start the app

```bash
./mvnw spring-boot:run
```
App will be available at `http://localhost:8080`.

---
# Build and Run with Docker
```bash
ocker build -t budgetbot .

docker run --rm -p 8080:8080 \
  -e TELEGRAM_MINIAPP_URL=... \
  -e YNAB_CLIENT_ID=... \
  -e YNAB_CLIENT_SECRET=... \
  -e YNAB_REDIRECT_URI=... \
  -e BUDGETBOT_JOB_TOKEN=... \
  budgetbot
```

For non-`dev`, also pass AWS/DynamoDB/KMS settings and activate the target Spring profile.

---

## Testing and Quality

```bash
./mvnw test
```

Additionally configured in the project:
- `failsafe` for integration tests (`*IT.java`);
- `JaCoCo` with merged unit + integration coverage;
- `PIT` mutation testing during `verify`.

Full verification:

```bash
./mvnw verify
```

---
