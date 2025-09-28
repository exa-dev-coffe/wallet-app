# ====== Build Stage ======
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml dan download dependency dulu (biar cache lebih efisien)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code dan build
COPY src ./src
RUN mvn clean package -DskipTests

# ====== Run Stage ======
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Copy jar hasil build dari stage sebelumnya
COPY --from=build /app/target/*.jar app.jar

# Expose port (ubah sesuai port Spring Boot kamu, default 8080)
EXPOSE 8080

# Jalankan aplikasi
ENTRYPOINT ["java","-jar","app.jar"]
