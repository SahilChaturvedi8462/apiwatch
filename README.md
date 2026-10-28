# APIWatch — API Health Monitoring System

APIWatch is a Spring Boot 3 REST API application that allows authenticated users to register APIs for monitoring, automatically checks endpoint health on a configurable schedule, stores historical check results in PostgreSQL, and alerts users via email when an endpoint transitions between UP and DOWN states.

---

## Technology Stack

- **Java**: 17 (OpenJDK / Temurin)
- **Framework**: Spring Boot 3.3.5
- **Build Tool**: Maven 3.9+
- **Database**: PostgreSQL 14+
- **Database Migrations**: Flyway
- **Security**: Spring Security + JJWT (0.12.6)
- **API Documentation**: SpringDoc OpenAPI 3 / Swagger UI
- **Testing**: JUnit 5, Mockito, Spring Test

---

## Prerequisites

1. **Java 17 SDK** installed (`java -version`).
2. **Maven 3.9+** installed (`mvn -version`).
3. **PostgreSQL** running locally on port `5432`.

---

## Local Database Setup

Connect to your local PostgreSQL instance and create the database:

```sql
CREATE DATABASE apiwatch_db;
```

Flyway will automatically execute migrations located in `src/main/resources/db/migration` upon application launch, creating the `users`, `monitored_endpoints`, and `health_checks` tables.

---

## Configuration & Environment Variables

Copy `.env.example` to `.env` or export environment variables:

| Variable | Description | Default |
|---|---|---|
| `SERVER_PORT` | HTTP server port | `8080` |
| `DB_HOST` | PostgreSQL host | `localhost` |
| `DB_PORT` | PostgreSQL port | `5432` |
| `DB_NAME` | Database name | `apiwatch_db` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `JWT_SECRET` | 256-bit secret key for HMAC-SHA256 | *Default test key* |
| `JWT_EXPIRATION_MS`| Token lifetime in milliseconds | `86400000` (24h) |
| `SCHEDULER_INTERVAL_MS`| Interval between health polling jobs | `60000` (1 min) |
| `MAIL_HOST` | SMTP server host | `smtp.gmail.com` |
| `MAIL_PORT` | SMTP server port | `587` |
| `MAIL_USERNAME` | SMTP account email | *(empty)* |
| `MAIL_PASSWORD` | SMTP app password | *(empty)* |
| `MAIL_FROM` | Sender email address | `no-reply@apiwatch.com` |
| `MAIL_ENABLED` | Toggle email sending | `false` |

---

## Building and Running

### Build Project
```bash
mvn clean compile
```

### Run Tests
```bash
mvn test
```

### Run Application
```bash
mvn spring-boot:run
```

Once running, verify the service status:
```bash
curl http://localhost:8080/ping
```

Expected response:
```json
{
  "status": "UP",
  "service": "APIWatch",
  "message": "pong"
}
```

---

## Project Structure Overview

```
src/main/java/com/apiwatch
├── ApiWatchApplication.java       # Application entry point
├── config/                         # Security, OpenAPI, Scheduler, Mail configs
├── controller/                     # REST controllers
├── dto/                            # Data Transfer Objects (Requests & Responses)
├── entity/                         # JPA entities (User, MonitoredEndpoint, HealthCheck)
├── exception/                      # Global exception handler & domain exceptions
├── repository/                     # Spring Data JPA repositories
├── scheduler/                      # Background health-check scheduler
├── security/                       # JWT service, filters, authentication logic
├── service/                        # Business logic interfaces & implementations
└── validator/                      # SSRF URL safety validator

src/main/resources
├── application.yml                 # Application configuration & env bindings
└── db/migration/
    └── V1__init_schema.sql         # Flyway initial schema migration
```
