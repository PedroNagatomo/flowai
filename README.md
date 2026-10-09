# FlowAI

**Describe your automation. Let AI build the workflow.**

FlowAI is a self-hostable workflow automation platform that turns natural language instructions into executable workflows. Instead of manually connecting nodes through a drag-and-drop interface, you describe what you want to automate, review the generated workflow, and activate it.

Built with Java, Spring Boot, React, and TypeScript, FlowAI combines AI-powered workflow generation with an asynchronous execution engine, external service integrations, conditional branching, and persistent execution logs.

The goal is simple: make workflow automation easier to create, inspect, run, and self-host.

## Features

* **AI-powered workflow generation:** Convert natural language instructions into structured workflow definitions using an LLM.
* **No drag-and-drop required:** Generate workflows from descriptions and review or edit their JSON definitions directly.
* **Two execution models:** Run workflows as sequential action lists or directed graphs with conditional branching and fan-out.
* **Multiple triggers:** Start workflows through webhooks, cron schedules, or manual execution.
* **External integrations:** Send emails, post Slack messages, make HTTP requests, and execute AI prompts.
* **Conditional logic:** Evaluate runtime values using comparison and string-matching operators.
* **Variable interpolation:** Pass trigger payloads and previous node outputs between workflow steps.
* **Execution history:** Persist run status, duration, payloads, and step-level logs.
* **JWT authentication:** Secure API access with Spring Security.
* **Self-hosted deployment:** Run the platform using Docker Compose, PostgreSQL, and Nginx.
* **Extensible architecture:** Add new action types through Spring components implementing a shared execution interface.

## Tech Stack

| Layer               | Technologies                              |
| ------------------- | ----------------------------------------- |
| Backend             | Java 17, Spring Boot 3.3.5                |
| Security            | Spring Security, JWT                      |
| Persistence         | Spring Data JPA, Hibernate, PostgreSQL 16 |
| Database migrations | Flyway                                    |
| Email               | Spring Mail                               |
| Scheduling          | cron-utils                                |
| HTTP clients        | Spring RestClient                         |
| Frontend            | React 18, TypeScript 5, Vite 5            |
| Styling             | Tailwind CSS 3                            |
| State management    | Zustand                                   |
| Routing             | React Router DOM                          |
| HTTP requests       | Axios                                     |
| AI                  | Groq API, `openai/gpt-oss-120b`           |
| Infrastructure      | Docker, Docker Compose, Nginx             |
| CI                  | GitHub Actions                            |

The LLM integration is abstracted behind a client interface, allowing the provider to be replaced without redesigning the workflow execution engine.

## Architecture Overview

FlowAI separates workflow definition, execution, and persistence into distinct responsibilities.

1. **Workflow creation:** Users describe an automation in natural language or write a workflow definition manually.
2. **AI generation:** The LLM converts the description into a structured workflow definition.
3. **Review and activation:** Users inspect and edit the generated JSON before activating the workflow.
4. **Trigger dispatch:** A webhook, scheduled event, or manual request initiates execution.
5. **Workflow execution:** The engine selects the linear or graph execution strategy and runs the appropriate actions.
6. **Persistence and observability:** Execution results and step-level logs are stored for later inspection.

### Execution Models

FlowAI supports two mutually exclusive execution formats.

**Linear execution**

A workflow containing an `actions` array executes each action sequentially. This format is suited to straightforward automations that do not require branching.

**Graph execution**

A workflow containing a `graph` object executes nodes connected through `next` references. This format supports conditional branches, fan-out, and data dependencies between nodes.

When a graph is present, the graph executor takes precedence over the linear executor.

## Workflow Definition

Every workflow uses a JSON definition with a common top-level structure:

```json
{
  "trigger": {
    "type": "WEBHOOK",
    "config": {}
  },
  "conditions": [],
  "actions": []
}
```

The example above illustrates the linear format. Graph-based workflows use a `graph` object instead of the `actions` array.

### Triggers

| Trigger          | Description                                              | Status                                     |
| ---------------- | -------------------------------------------------------- | ------------------------------------------ |
| `WEBHOOK`        | Starts a workflow when an HTTP POST request is received. | Implemented                                |
| `SCHEDULE`       | Runs a workflow according to a cron schedule.            | Implemented                                |
| `MANUAL`         | Starts a workflow through the UI or an API request.      | Implemented                                |
| `EMAIL_RECEIVED` | Intended to trigger workflows when an email arrives.     | Not yet wired to an inbound email listener |

Webhook endpoint:

```text
POST /api/webhooks/{workflowId}
```

The request body becomes the trigger payload available to subsequent workflow steps.

Scheduled workflows are checked by a background scheduler, which recalculates upcoming execution times and dispatches due runs asynchronously.

### Actions

Each action is implemented as a Spring component that implements the shared `ActionExecutor` interface.

| Action         | Description                                                                        |
| -------------- | ---------------------------------------------------------------------------------- |
| `SEND_EMAIL`   | Sends an email through the configured SMTP server.                                 |
| `SEND_SLACK`   | Posts a message through a Slack incoming webhook.                                  |
| `HTTP_REQUEST` | Sends an HTTP request to an external URL.                                          |
| `AI_PROMPT`    | Sends a prompt to the configured LLM and exposes its response to subsequent nodes. |

Slack webhook configuration can be supplied through the action configuration, a user-configured integration, or a global environment variable.

To introduce a new action, implement the execution interface and register the corresponding action type.

## Conditional Branching

Graph workflows support conditional execution through `IF` nodes.

A node can reference its next destination using a single node ID:

```json
{
  "next": "n2"
}
```

It can also dispatch execution to multiple nodes:

```json
{
  "next": ["n2", "n3"]
}
```

For conditional branches, an `IF` node can define separate destinations:

```json
{
  "next": {
    "then": ["n4"],
    "else": ["n5"]
  }
}
```

The executor evaluates the condition against the current execution context and follows the corresponding branch.

### Supported Condition Operators

* `EQUALS`
* `NOT_EQUALS`
* `CONTAINS`
* `NOT_CONTAINS`
* `STARTS_WITH`
* `ENDS_WITH`
* `GREATER_THAN`
* `LESS_THAN`
* `IS_EMPTY`
* `IS_NOT_EMPTY`

Nodes execute sequentially along the selected execution paths. Loops are not supported in the current version, and cycle detection stops execution when a cycle is encountered.

## Variable Interpolation

Workflow configuration strings can reference runtime values using double-brace expressions.

| Expression                  | Description                                |
| --------------------------- | ------------------------------------------ |
| `{{trigger.payload}}`       | The complete trigger payload.              |
| `{{trigger.payload.field}}` | A specific field from the trigger payload. |
| `{{nodes.nodeId.output}}`   | The output of a previously executed node.  |
| `{{workflow.name}}`         | The workflow name.                         |
| `{{now}}`                   | The current timestamp.                     |
| `{{now.date}}`              | The current date.                          |
| `{{now.time}}`              | The current time.                          |

Interpolation occurs immediately before each action executes, making outputs from earlier nodes available to subsequent steps.

For example, an AI node can classify an incoming webhook payload, and a later Slack action can use the classification result to construct its notification.

## Getting Started

### Prerequisites

Before running FlowAI, make sure you have:

* Docker and Docker Compose v2.
* A Groq API key for AI workflow generation and the `AI_PROMPT` action.
* Java 17 and Maven, if running the backend outside Docker.
* Node.js 20 and npm, if running the frontend outside Docker.
* An SMTP server, if you want to use `SEND_EMAIL`.
* A Slack incoming webhook URL, if you want to use `SEND_SLACK`.

Obtain a Groq API key from the [Groq Console](https://console.groq.com).

### 1. Clone the repository

```bash
git clone https://github.com/PedroNagatomo/flowai.git
cd flowai
```

### 2. Configure environment variables

Copy the example environment file:

```bash
cp .env.example .env.prod
```

Edit `.env.prod` and configure the required values:

```dotenv
DB_NAME=flowai
DB_USER=flowai
DB_PASS=your-strong-database-password

JWT_SECRET=your-generated-jwt-secret

GROQ_API_KEY=your-groq-api-key

CORS_ORIGINS=http://localhost
FRONTEND_PORT=80
```

Generate secure random values:

```bash
openssl rand -base64 24
openssl rand -hex 32
```

Use the first command for a database password and the second for a JWT secret, provided the application's secret configuration accepts the generated format and length.

Optional SMTP configuration:

```dotenv
MAIL_HOST=smtp.example.com
MAIL_PORT=587
MAIL_USERNAME=user@example.com
MAIL_PASSWORD=your-smtp-password
```

Optional Slack configuration:

```dotenv
SLACK_DEFAULT_WEBHOOK_URL=https://hooks.slack.com/services/your/webhook
```

Use the exact variable names supported by `.env.example` and the application configuration. Keep `.env.prod` out of version control and never commit API keys, passwords, or production secrets.

### 3. Build and start the application

Run the production Docker Compose configuration:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml up -d --build
```

The deployment starts three containers:

| Container         | Responsibility                                                                          |
| ----------------- | --------------------------------------------------------------------------------------- |
| `flowai-postgres` | Stores application and workflow data.                                                   |
| `flowai-backend`  | Runs the Spring Boot API and workflow engine on port 8080 inside the Docker network.    |
| `flowai-frontend` | Serves the React application through Nginx and proxies `/api/` requests to the backend. |

### 4. Verify the deployment

Check container status:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml ps
```

Inspect logs if a service fails to start:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml logs -f
```

On the first startup, the backend may take approximately 30–60 seconds to complete database migrations and JVM initialization. Actual startup time depends on the environment.

Once the services are healthy, open:

```text
http://localhost
```

Create an account and start building your first workflow.

### 5. Stop the application

Stop the containers while preserving the database volume:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down
```

To remove the database volume as well:

```bash
docker compose --env-file .env.prod -f docker-compose.prod.yml down -v
```

**Warning:** The `-v` option removes persisted volumes managed by the Compose project. Make sure you have a backup if you need to preserve your data.

## Example Automation

Consider a workflow that classifies incoming messages and notifies a team when negative sentiment is detected.

**Workflow sequence**

1. Receive a POST request through a webhook.
2. Send the message to an AI prompt for sentiment classification.
3. Evaluate the classification using an `IF` node.
4. Send a Slack notification if the result indicates negative sentiment.
5. Persist the execution status and step logs.

This example demonstrates how triggers, AI actions, conditional branching, external integrations, and execution history work together.
