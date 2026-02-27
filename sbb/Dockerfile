# ---- build stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
WORKDIR /app/sbb
RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/app.jar"]
