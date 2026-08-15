# Wallet Service (wallet-service)

Wallet Service is a financial microservice built with **Java 17** and **Spring Boot 3**. It handles user balances, top-ups via payment gateways, and internal checkout deductions.

## 🚀 Technologies

*   **Language**: Java 17
*   **Framework**: Spring Boot 3.4
*   **Database**: PostgreSQL
*   **ORM**: Spring Data JPA / Hibernate
*   **Cache**: Redis (Spring Data Redis)
*   **External API**: Midtrans (Payment Gateway)
*   **Observability**: OpenTelemetry
*   **Logging**: Logback (JSON Format)
*   **Build Tool**: Maven

## 📦 Features

*   **Balance Management**: Securely handles user balances.
*   **Payment Top-up**: Integrates with Midtrans Snap API for digital top-ups.
*   **Internal Payment**: Validates HMAC signatures from `transaction-service` to deduct balances during checkout securely.
*   **PIN Validation**: Enforces secure PIN checks before allowing transactions.

## 🛠️ Prerequisites

*   JDK 17
*   Maven 3.8+
*   PostgreSQL
*   Redis

## ⚙️ Environment Variables

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

## 🚀 How to Run

1.  **Local Development:**
    ```bash
    ./mvnw spring-boot:run
    ```

2.  **Build Docker Image:**
    ```bash
    docker build -t eka-dev/wallet-service .
    ```
