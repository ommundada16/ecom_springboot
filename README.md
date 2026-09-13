# ShopEase

A full-stack e-commerce product catalog application with a Spring Boot REST API backend and a React (Vite) frontend.

## Features

- Browse products in a responsive grid, filterable by category
- Live search-as-you-type across name, description, brand, and category
- Product detail page with image, stock, and pricing
- Add, update, and delete products, including image upload
- Shopping cart with quantity controls, persisted to local storage
- Checkout flow that updates stock on confirmation
- Light/dark theme toggle

## Tech Stack

**Backend**
- Java 21, Spring Boot 3.3
- Spring Data JPA / Hibernate
- H2 in-memory database
- Maven

**Frontend**
- React 18 + Vite
- React Router
- Axios
- Bootstrap 5 / React-Bootstrap

## Project Structure

```
.
├── backend/    Spring Boot REST API
└── frontend/   React + Vite client
```

## Getting Started

### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. Sample product data is seeded automatically on startup into an in-memory H2 database.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The app runs on `http://localhost:5173` and talks to the backend on `http://localhost:8080`.

## API Overview

| Method | Endpoint                        | Description                  |
|--------|----------------------------------|-------------------------------|
| GET    | `/api/products`                 | List all products             |
| GET    | `/api/product/{id}`             | Get a single product          |
| GET    | `/api/product/{id}/image`       | Get a product's image         |
| POST   | `/api/product`                  | Create a product (multipart)  |
| PUT    | `/api/product/{id}`             | Update a product (multipart)  |
| DELETE | `/api/product/{id}`             | Delete a product               |
| GET    | `/api/products/search?keyword=` | Search products                |
