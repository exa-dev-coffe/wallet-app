# 💳 Wallet Service (`wallet-service`)

Wallet Service is a financial microservice built with **Java 17** and **Spring Boot 3**. It handles user balances, top-ups via payment gateways, and internal checkout deductions.

## 🚀 Technologies

- **Language**: Java 17
- **Framework**: Spring Boot 3.4
- **Database**: PostgreSQL
- **Migration**: Liquibase
- **ORM**: Spring Data JPA / Hibernate
- **Cache**: Redis (Spring Data Redis)
- **External API**: Midtrans (Payment Gateway)
- **Observability**: OpenTelemetry
- **Logging**: Logback (JSON Format)
- **Build Tool**: Maven

## 📦 Features

- **Balance Management**: Securely handles user balances.
- **Payment Top-up**: Integrates with Midtrans Snap API for digital top-ups.
- **Internal Payment**: Validates HMAC signatures from `transaction-service` to deduct balances during checkout securely.
- **PIN Validation**: Enforces secure PIN checks before allowing transactions.

## 🛠️ Prerequisites

- JDK 17
- Maven 3.8+
- PostgreSQL
- Redis

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

## 🗄️ Database Migration (Liquibase)

Service ini menggunakan **Liquibase** untuk database migration. Dokumentasi lengkap cara menjalankan dan membuat file migration tersedia di **[DOCS_MIGRATION.md](file:///d:/Project/coffe/wallet-service/DOCS_MIGRATION.md)**.

- **Otomatis**: Dijalankan otomatis oleh Spring Boot saat aplikasi startup (`spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml`).
- **Manual (CLI)**: `./mvnw liquibase:update`
- **Rollback (CLI)**: `./mvnw liquibase:rollback -Dliquibase.rollbackCount=1`

## 🧪 Integration Testing

Jalankan perintah berikut untuk menguji seluruh endpoint saldo, top-up, deduct, dan webhook Midtrans:

```bash
# Windows
.\mvnw.cmd test

# Linux/macOS
./mvnw test
```

*Requirement:* Docker Desktop/Daemon must be running.
