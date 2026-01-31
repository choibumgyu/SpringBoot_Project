# ---- build stage ----
FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY . .
RUN gradle clean bootJar -x test

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/app.jar"]
