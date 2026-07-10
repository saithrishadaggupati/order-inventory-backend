# Order Inventory Backend

A production-oriented backend service for managing products and inventory workflows. Built with Spring Boot and Java, the project includes JWT authentication, role-based authorization, Redis caching, PostgreSQL persistence, database migrations, and containerized deployment.

## Tech Stack

- Backend: Java 17, Spring Boot 3.5
- Database: PostgreSQL on AWS RDS
- Caching: Redis
- Security: Spring Security, JWT authentication
- Database Migrations: Flyway
- Containerization: Docker, Docker Compose
- Deployment: AWS EC2

## Key Features

- User registration and login with JWT-based authentication
- Role-based access control with "USER" and "ADMIN" roles
- Product creation and retrieval APIs
- Redis caching for frequently requested product data
- Outbox pattern support for reliable event processing
- Idempotency key handling to make request retries safer
- Version-controlled database schema changes with Flyway
- Docker-based local setup and deployment workflow

## API Endpoints

| Method | Endpoint | Access | Description |
|--------|----------|--------|-------------|
| POST | /auth/register | Public | Register a new user |
| POST | /auth/login | Public | Authenticate and receive a JWT |
| GET | /products/{id} | Public | Fetch a product by ID |
| POST | /products | Admin only | Create a new product |

## Running Locally

### Prerequisites

Before getting started, make sure you have:

- Docker
- Docker Compose
- Access to a PostgreSQL database, such as AWS RDS

### Setup

1. Clone the repository:

\`\`\`bash
git clone https://github.com/saithrishadaggupati/order-inventory-backend.git
cd order-inventory-backend
\`\`\`

2. Create a ".env" file in the project root:

\`\`\`
DB_PASSWORD=your_db_password_here
\`\`\`

3. If needed, update "docker-compose.yml" with your PostgreSQL or AWS RDS connection details.

4. Build and start the services:

\`\`\`bash
docker compose up -d --build
\`\`\`

5. Once the application is running, the API will be available at:

\`\`\`
http://localhost:8080
\`\`\`

## Environment Variables

| Variable | Description |
|----------|--------------|
| DB_PASSWORD | PostgreSQL database password. Keep this value private and never commit it to version control. |

## Project Notes

- Users created through the registration endpoint are assigned the "USER" role by default.
- The "ADMIN" role must be assigned separately and cannot be selected during public registration.
- Database schema changes are managed through Flyway migrations located in "src/main/resources/db/migration".
- Sensitive configuration values should be stored in environment variables and excluded from Git.

## Why This Project?

This project explores backend patterns commonly used in real-world services, including authentication, authorization, caching, reliable event handling, idempotent request processing, database migrations, and containerized deployment.
