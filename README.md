# Problem
Microservices often require distributed transactions across multiple services.
Traditional database transactions fail because:

- **Services are independent**

- **Network failures happen**

- **Partial success must be compensated**

**Solution**: implement the **Saga Pattern**.

### Key Technologies
| Category | Stack                                                        |
|-----------|--------------------------------------------------------------|
| Integration | Apache Camel (v4.x)                                          |
| Messaging | Apache Kafka                                                 |
| Persistence | PostgreSQL (Orders), MySQL (Inventory), MongoDB (Notifications) |
| Runtime | Spring Boot, Docker                                          |
| Patterns | Idempotency, Saga Choreography, DLQ                          |

### The Workflow:
1. **Order Service** (REST) receives an order → persists it as `PENDING` → publishes to Kafka (`INVENTORY_CHECK_TOPIC`)
2. **Inventory Service** consumes the event → checks stock in MySQL →
    - if in stock → publishes to `PAYMENT_REQUEST_TOPIC`
    - if out of stock → updates order to `DECLINED` (Postgres)
3. **Payment Service** consumes `PAYMENT_REQUEST_TOPIC` → simulates payment → saves to DB → publishes to `PAYMENT_STATUS_TOPIC`
4. **Notification Service** consumes `PAYMENT_STATUS_TOPIC` → stores notification logs in **MongoDB** → publishes to `ORDER_SHIPPING_TOPIC` (future extension)

---

### How to Run Locally

## Prerequisites
- **Docker & Docker Compose**

- **Java 17+**

- **Maven 3.8+**

- Step-by-Step Setup:

- **Build the Microservices:**

From the root directory, run the build script to compile the JARs, build and run the containers:
```
chmod +x docker.sh
./docker.sh
```

```
docker compose up -d
docker compose ps
```

## Architecture
- **Pattern:** Choreographed, event-driven pipeline
- **Transport:** Apache Kafka
- **Orchestration:** Apache Camel routes in each microservice

## Services

### 1. Order Service
- **Endpoint:** `POST /orders/{userId}/{variantProductId}`
- **Body:**
  ```json
  {
    "quantity": 10,
    "totalPrice": 696
  }

---

## Order Created (Sent to INVENTORY_CHECK_TOPIC)
``` json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user_99",
  "variantProductId": 28,
  "quantity": 2,
  "totalPrice": 150.50
}

```
## Payment Requested (Sent to PAYMENT_REQUEST_TOPIC)
``` json

{
"orderId": "550e8400-e29b-41d4-a716-446655440000",
"status": "STOCK_RESERVED",
"amount": 150.50
}
```


