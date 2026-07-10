# Order Inventory Backend

A concurrency-safe backend service for order and inventory management. Built with Spring Boot and Java, this project focuses on correctness under concurrent load: optimistic locking prevents overselling, a transactional outbox guarantees reliable event publishing, and idempotency keys make order placement safe to retry.

## Tech Stack

- Backend: Java 17, Spring Boot 3.5
- Database: PostgreSQL on AWS RDS
- Caching: Redis
- Security: Spring Security, JWT authentication
- Database Migrations: Flyway
- Containerization: Docker, Docker Compose
- Deployment: AWS EC2

## Key Features

- **Order placement with concurrency safety** — orders decrement product stock using optimistic locking (`@Version`), preventing overselling under concurrent requests
- **Idempotent order creation** — orders can include an `Idempotency-Key` header; retried requests with the same key return the original response instead of creating a duplicate order
- **Transactional outbox pattern** — order placement writes domain events (including low-stock alerts) to an outbox table, published asynchronously by a background `OutboxPublisher`, avoiding dual-write inconsistency between the DB and event stream
- User registration and login with JWT-based authentication
- Role-based access control (`USER`, `ADMIN`)
- Redis caching for frequently requested product data
- Concurrency test suite (`OrderConcurrencyTest`) validating safe stock decrements under simultaneous order requests

## API Endpoints

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | /auth/register | Public | Register a new user |
| POST | /auth/login | Public | Authenticate and receive a JWT |
| GET | /products/{id} | Public | Fetch a product by ID |
| POST | /products | Admin only | Create a new product |
| POST | /orders | Authenticated | Place an order (optional `Idempotency-Key` header for safe retries) |

## Running Locally

### Prerequisites

- Docker
- Docker Compose
- Access to a PostgreSQL database, such as AWS RDS

### Setup

1. Clone the repository:

\`\`\`bash
git clone https://github.com/saithrishadaggupati/order-inventory-backend.git
cd order-inventory-backend
\`\`\`

2. Create a `.env` file in the project root:

\`\`\`
DB_PASSWORD=your_db_password_here
\`\`\`

3. If needed, update `docker-compose.yml` with your PostgreSQL or AWS RDS connection details.

4. Build and start the services:

\`\`\`bash
docker compose up -d --build
\`\`\`

5. The API will be available at:

\`\`\`
http://localhost:8080
\`\`\`

## Example: Placing an Idempotent Order

\`\`\`bash
curl -X POST http://localhost:8080/orders \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer <your_jwt>" \\
  -H "Idempotency-Key: unique-key-123" \\
  -d '{"productId": 1, "quantity": 2}'
\`\`\`

Repeating this exact request with the same `Idempotency-Key` returns the original order response rather than creating a second order.

## Environment Variables

| Variable | Description |
|----------|--------------|
| DB_PASSWORD | PostgreSQL database password. Keep this value private and never commit it to version control. |

## Project Notes

- Users created through the registration endpoint are assigned the `USER` role by default; `ADMIN` must be assigned separately (e.g. directly in the database).
- Database schema changes are managed through Flyway migrations located in `src/main/resources/db/migration`.
- Sensitive configuration values are stored in environment variables and excluded from Git via `.gitignore`.

## Roadmap

- [ ] k6 load testing for order placement under concurrent traffic
- [ ] GitHub Actions CI/CD pipeline
- [ ] Expand order endpoints (order history, cancellation, status updates)

## Why This Project?

This project was built to demonstrate concurrency-safe backend patterns relevant to real-world order/inventory systems: preventing overselling under simultaneous requests, avoiding dual-write failures between database and event stream, and making write operations safe to retry over unreliable networks.
