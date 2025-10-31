# Async Event-Driven Order Pipeline

This project demonstrates a small, production-ish, event-driven system built with **Spring Boot + Apache Camel + Kafka** and multiple datastores (**PostgreSQL, MySQL, MongoDB**).

It models a simple e-commerce flow:

1. **Order Service** (REST) receives an order → persists it as `PENDING` → publishes to Kafka (`INVENTORY_CHECK_TOPIC`)
2. **Inventory Service** consumes the event → checks stock in MySQL →
    - if in stock → publishes to `PAYMENT_REQUEST_TOPIC`
    - if out of stock → updates order to `DECLINED` (Postgres)
3. **Payment Service** consumes `PAYMENT_REQUEST_TOPIC` → simulates payment → saves to DB → publishes to `PAYMENT_STATUS_TOPIC`
4. **Notification Service** consumes `PAYMENT_STATUS_TOPIC` → stores notification logs in **MongoDB** → publishes to `ORDER_SHIPPING_TOPIC` (future extension)

---

## Architecture

- **Pattern:** Choreographed, event-driven pipeline
- **Transport:** Apache Kafka
- **Orchestration:** Apache Camel routes in each microservice
- **Persistence:**
    - Orders → PostgreSQL
    - Products / Stocks → MySQL
    - Notifications / audit → MongoDB

See `/diagrams/architecture.drawio` for the visual.

---

## Services

### 1. Order Service
- **Endpoint:** `POST /orders/{userId}/{variantProductId}`
- **Body:**
  ```json
  {
    "quantity": 10,
    "totalPrice": 696
  }
