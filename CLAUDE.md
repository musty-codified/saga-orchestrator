# CLAUDE.md - Production Hardening & Architectural Strategy

## Core Constraints & Technology Stack
- **Languages/Frameworks:** Java, Spring Boot, Apache Camel.
- **Middleware:** Apache Kafka (Event Broker), Camel Routes handle all inter-service integration.
- **Datastores:** PostgreSQL (Orders, Payments), MySQL (Inventory), MongoDB (Notifications).
- **Target Runtime:** Local (Docker Compose), Production (Kubernetes/Minikube).

## Architectural Mandate: Demo to Production-Grade
When suggesting code changes, refactors, or new files, enforce these enterprise-grade patterns directly into the implementation:

### 1. Data Consistency & Reliability Patterns
- **Transactional Outbox:** Always persist outgoing Kafka events to a local relational `outbox` table within the same database transaction as the business logic entity.
- **Idempotent Consumers (Inbox Pattern):** Enforce idempotent message processing using an `inbox` deduplication table before executing downstream actions.
- **Error Handling & Resiliency:** Every Camel route must explicitly declare error handlers with exponential backoff retries, explicit timeouts, and fallback routing to a structured Dead-Letter Queue (DLQ).

### 2. Saga Design & Distributed Transactions
- **Compensating Actions:** All multi-service flows must incorporate explicit rollback mechanisms for failures (e.g., if payment fails, issue an inventory void/restock event).
- **Architecture Strategy:** Favor migrating from decentralized choreography to a structured Saga Orchestrator where state management is centralized for critical order lifecycles.
- **Inventory Reservation:** Implement a pessimistic or state-checked inventory reservation lifecycle rather than direct subtraction to handle high concurrency.
- **Payment Lifecycle:** Break payment handling into explicit `Authorize`, `Capture`, and `Void` phases.

### 3. Structural & Production Guidelines
- **Camel Route Hygiene:** Keep business logic strictly isolated in Spring-managed service beans; Camel routes must purely handle enterprise integration patterns (EIP), error policies, and data transforming.
- **Concurrency & Scaling:** Design schemas and code to withstand high-concurrency race conditions (e.g., inventory flash sales). Use database locking strategies or optimistic locking version fields where appropriate.
- **Observability:** Include hooks for distributed tracing headers (W3C/OpenTelemetry) across Kafka/Camel boundaries and emit structured JSON logging.
- **Service Decomposition:** Separate Inventory 

## Common Automation Commands
- **Build Services:** `mvn clean install` 
- **Spin up Infrastructure:** `docker-compose up -d`
- **Run Tests:** `mvn test`