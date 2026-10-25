# ShopEase

A full-stack e-commerce application: a Spring Boot REST API with JWT authentication, role-based access and persisted orders on PostgreSQL, plus a React single-page frontend. The whole stack runs with one `docker compose up`.

## Features

- **Catalog** with server-side pagination, sorting, category filtering and search-as-you-type
- **Authentication**: registration and login with BCrypt-hashed passwords and stateless JWTs
- **Roles**: customers browse and order; only admins can create, update or delete products
- **Orders**: checkout is persisted per user, with order history. Stock is decremented inside a single transaction using row locks, so concurrent checkouts can never oversell
- **Validation and error handling**: Bean Validation on every request body and a global handler that returns consistent JSON errors (400, 401, 403, 404, 409)
- **API docs**: interactive Swagger UI generated from the code
- **Versioned database schema** with Flyway migrations; data survives restarts

## Tech stack

| Layer | Technologies |
|---|---|
| Backend | Java 21, Spring Boot 3.3, Spring Web, Spring Data JPA / Hibernate, Spring Security, Bean Validation, Lombok |
| Auth | JWT (jjwt), BCrypt |
| Database | PostgreSQL 16, Flyway migrations |
| API docs | springdoc-openapi (Swagger UI) |
| Frontend | React 18, Vite, React Router, Axios, Bootstrap 5 |
| Testing | JUnit 5, Mockito, Spring MockMvc, Spring Security Test, H2 (PostgreSQL mode) |
| DevOps | Docker, Docker Compose, nginx |

## Quick start (Docker)

```bash
docker compose up --build
```

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |

A demo admin account is created on first startup (`admin@shopease.com` / `Admin@12345`). Register a normal account from the UI to try the customer flow.

Every setting has a development default. To change ports, credentials or the JWT secret, copy `.env.example` to `.env` and edit it. **Set your own `JWT_SECRET` and admin credentials for any real deployment.**

## Running locally without Docker

You need Java 21+, Node 20+ and a PostgreSQL database (the compose file can provide one: `docker compose up db`).

```bash
# Backend (http://localhost:8080)
cd backend
./mvnw spring-boot:run

# Frontend (http://localhost:5173)
cd frontend
npm install
npm run dev
```

The backend reads these environment variables (defaults in `application.properties`):

| Variable | Purpose | Default |
|---|---|---|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | PostgreSQL connection | `jdbc:postgresql://localhost:5432/shopease`, `shopease`, `shopease` |
| `JWT_SECRET` | HMAC signing key, at least 32 characters | development-only value |
| `JWT_EXPIRATION_MINUTES` | Token lifetime | `60` |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins | `http://localhost:5173,http://localhost:3000` |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Admin account created at startup | `admin@shopease.com`, `Admin@12345` |

The frontend reads `VITE_API_URL` (default `http://localhost:8080/api`).

## Tests

```bash
cd backend
./mvnw test
```

The suite covers the service layer with Mockito, the full HTTP stack with MockMvc (real security filter chain, real JWTs, real Flyway schema), and a concurrency test that fires simultaneous checkouts at limited stock and asserts no overselling. Integration tests run on H2 in PostgreSQL mode and apply the same Flyway migrations, so no database needs to be running.

## API overview

Full interactive documentation is in Swagger UI. Authenticated endpoints expect `Authorization: Bearer <token>`.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | public | Create a customer account, returns a JWT |
| POST | `/api/auth/login` | public | Log in, returns a JWT |
| GET | `/api/products` | public | Paginated list: `?page=0&size=8&sort=price,desc&category=Laptop` |
| GET | `/api/products/{id}` | public | Get one product |
| GET | `/api/products/search?keyword=` | public | Search name, description, brand, category (max 10 results) |
| POST | `/api/products` | ADMIN | Create a product |
| PUT | `/api/products/{id}` | ADMIN | Update a product |
| DELETE | `/api/products/{id}` | ADMIN | Delete a product (order history is kept) |
| POST | `/api/orders` | logged in | Place an order; 409 if stock is insufficient |
| GET | `/api/orders` | logged in | My orders, newest first |
| GET | `/api/orders/{id}` | logged in | One of my orders (admins can read any) |

Errors share one shape:

```json
{
  "timestamp": "2026-10-25T11:02:39Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Name is required; Price must be greater than zero",
  "path": "/api/products",
  "fieldErrors": { "name": "Name is required", "price": "Price must be greater than zero" }
}
```

## Design notes

- **No overselling.** `OrderService.placeOrder` runs in one transaction and loads the affected product rows with `SELECT ... FOR UPDATE`, ordered by id so concurrent transactions lock in the same order and cannot deadlock. All lines are validated before any stock changes, and a database `CHECK (stock_quantity >= 0)` is a second line of defence.
- **Order history survives product deletion.** Each order line stores a snapshot of the product name and unit price, and its product reference is set to null if the product is deleted.
- **Stateless security.** Sessions are disabled; the JWT carries the email and role. Unknown email and wrong password return the same 401 so accounts cannot be enumerated, and registration can never create an admin.
- **Schema owned by Flyway.** Hibernate only validates the schema (`ddl-auto=validate`); all changes go through versioned migrations in `backend/src/main/resources/db/migration`.
- **DTOs at the API boundary.** Entities are never serialised directly, so the database model can change without breaking the API.

## Project structure

```
.
├── backend/              Spring Boot API
│   └── src/main/java/com/ecommerce/ecom_proj
│       ├── config/       Security, CORS, OpenAPI, admin seeding
│       ├── controller/   REST controllers
│       ├── dto/          Request and response records
│       ├── exception/    Domain exceptions and global handler
│       ├── model/        JPA entities
│       ├── repo/         Spring Data repositories
│       ├── security/     JWT service and filter
│       └── service/      Business logic
├── frontend/             React + Vite client
├── docker-compose.yml    Postgres + backend + frontend
└── .env.example          Configuration template
```
