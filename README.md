# Order Inventory Backend

A backend for handling orders and inventory without the usual concurrency headaches — no overselling, no lost updates, no duplicate orders on retry. Built with Spring Boot, backed by Postgres, and load-tested to prove it actually holds up under pressure, not just in theory.

## Stack

- Java 17, Spring Boot 3.5
- PostgreSQL (AWS RDS)
- Redis for caching
- Spring Security + JWT
- Flyway for migrations
- k6 for load testing
- GitHub Actions for CI (build + tests run on every push)
- Docker Compose, deployed on AWS EC2

## What it does

**Orders don't oversell.** Stock updates use optimistic locking (`@Version` on the `Product` entity), so if two people try to buy the last item at the same time, only one wins — the other gets a clean conflict response instead of silently overselling.

**Retries are safe.** Order placement accepts an `Idempotency-Key` header. Send the same key twice — network hiccup, client retry, whatever — and you get back the original order, not a duplicate.

**Events don't get lost.** Placing an order writes to an outbox table in the same transaction as the order itself, and a background publisher (`OutboxPublisher`) picks those up and processes them separately. No dual-write problem where the DB commits but the event never fires.

Also included: JWT auth, role-based access (`USER`/`ADMIN`), Redis caching on product lookups.

## Does it actually work under load?

Ran a k6 test: 50 concurrent users, all hammering the same product that only has 30 units in stock.

- 30 orders succeeded — exactly matching stock
- 70 got rejected with a proper 409/400, not a crash
- Final stock: **0**, never negative
- p95 latency: 966ms, p99: 986ms

That's the whole point of the optimistic locking — it held under real concurrent traffic, not just a unit test running in isolation. Script's in `k6-tests/order-load-test.js`.

## Endpoints

| Method | Path | Who | What |
|--------|------|-----|------|
| POST | `/auth/register` | Anyone | Create an account |
| POST | `/auth/login` | Anyone | Get a JWT |
| GET | `/products/{id}` | Anyone | Look up a product |
| POST | `/products` | Admin | Create a product |
| POST | `/orders` | Logged in | Place an order (send `Idempotency-Key` to make retries safe) |

## Running it locally

\`\`\`bash
git clone https://github.com/saithrishadaggupati/order-inventory-backend.git
cd order-inventory-backend
\`\`\`

Create a `.env` file:
\`\`\`
DB_PASSWORD=your_db_password_here
\`\`\`

Point `docker-compose.yml` at your own Postgres instance if you're not using RDS, then:

\`\`\`bash
docker compose up -d --build
\`\`\`

API's up at `http://localhost:8080`.

## Placing an order

\`\`\`bash
curl -X POST http://localhost:8080/orders \\
  -H "Content-Type: application/json" \\
  -H "Authorization: Bearer <your_jwt>" \\
  -H "Idempotency-Key: unique-key-123" \\
  -d '{"productId": 1, "quantity": 2}'
\`\`\`

Send that exact request again with the same idempotency key and you'll get the same order back, not a second one.

## Running the load test

\`\`\`bash
k6 run -e LOAD_TEST_TOKEN=<your_jwt> ./k6-tests/order-load-test.js
\`\`\`

Token gets passed in at runtime — it's not sitting in the script.

## A few things worth knowing

- New users are always `USER` by default. Making someone `ADMIN` is a manual step right now (straight DB update).
- Schema changes go through Flyway (`src/main/resources/db/migration`).
- Nothing sensitive is committed — `.env` and any test tokens are gitignored.

## Not done yet

- More order endpoints — order history, cancellation, status tracking. Right now it's just placement, which was the priority given the time I had.

## Why I built this

I wanted to actually prove I could handle the hard part of order/inventory systems — the concurrency bugs that don't show up until you have real simultaneous traffic. It's easy to claim "handles concurrent requests safely" in a README; it's another thing to load test it and watch the numbers land exactly where they should.

## A note on latency and connection pooling

Tried bumping HikariCP's pool size from the default (10) to 30, expecting lower latency under the 50-VU load test. It got worse instead — p95 went from 966ms to 2.2s.

Turns out the bottleneck wasn't connections queuing, it was optimistic-lock contention on a single row. With a bigger pool, more requests hit the database at once, all racing to update the same product's stock — meaning more retries and conflicts happening at the database layer instead of queuing cleanly at the connection-pool layer. Reverted back to the default pool size, which performed better.

Takeaway: when 50 concurrent requests are all fighting over one row, adding more DB connections doesn't help — it just moves the contention downstream. The real fix for that kind of hot-row contention would be batching writes or serializing access with a queue, not scaling the connection pool.
