# 🗄️ Database Migration Documentation (Liquibase) - `wallet-service`

This microservice uses **Liquibase** for database schema management and migrations.

---

## 📁 Migration Structure

Migration files are stored in `src/main/resources/db/changelog`:

```
src/main/resources/db/changelog/
├── db.changelog-master.yaml         # Master changelog file linking all migration scripts
└── changes/
    └── 001_create_wallet_tables.sql # Liquibase SQL changeset migration script
```

---

## 🚀 Running Migrations

### Option 1: Automatic Migration on Application Startup (Recommended)

Liquibase automatically runs during Spring Boot application startup.

1. Ensure PostgreSQL database connection variables are configured in `.env` or `application.properties`:
   ```properties
   SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/wallet_db
   SPRING_DATASOURCE_USERNAME=postgres
   SPRING_DATASOURCE_PASSWORD=postgres
   spring.liquibase.change-log=classpath:db/changelog/db.changelog-master.yaml
   ```

2. Start the application:
   ```bash
   # Using run script
   ..\run-java.cmd .

   # Or using Maven
   ./mvnw spring-boot:run
   ```
   *Liquibase will check `DATABASECHANGELOG` table and apply any unapplied migration changesets automatically.*

---

### Option 2: Running Migrations via Maven Plugin (CLI)

You can run migration commands directly without launching the full Spring Boot application server:

#### Apply All Pending Migrations:
```bash
./mvnw liquibase:update
```

#### Check Migration Status:
```bash
./mvnw liquibase:status
```

#### Rollback Last Applied Migration:
```bash
./mvnw liquibase:rollback -Dliquibase.rollbackCount=1
```

---

## ✍️ How to Add a New Migration

1. Create a new `.sql` changeset file in `src/main/resources/db/changelog/changes/`:
   Example: `002_add_wallet_tier_column.sql`

2. Format the SQL file with Liquibase changeset metadata header:
   ```sql
   --liquibase formatted sql
   --changeset author_name:002_add_wallet_tier_column
   ALTER TABLE wallet ADD COLUMN tier VARCHAR(20) DEFAULT 'REGULAR';
   ```

3. Include the file in `src/main/resources/db/changelog/db.changelog-master.yaml`:
   ```yaml
   databaseChangeLog:
     - include:
         file: db/changelog/changes/001_create_wallet_tables.sql
     - include:
         file: db/changelog/changes/002_add_wallet_tier_column.sql
   ```

4. Restart the application or run `./mvnw liquibase:update`.
