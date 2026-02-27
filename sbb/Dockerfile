# ---- build stage ----
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# 1. 모든 파일 복사 (sbb 폴더 포함)
COPY . .

# 2. sbb 폴더로 이동하여 빌드 수행
WORKDIR /app/sbb
RUN chmod +x ./gradlew
# 빌드 시 테스트는 제외하고 jar 생성
RUN ./gradlew clean bootJar -x test

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# 3. 핵심 수정: builder 단계의 'sbb/build/libs'에서 가져와야 합니다.
# 만약 sbb 폴더 안에서 빌드했다면 결과물은 그 폴더 하위에 생깁니다.
COPY --from=builder /app/sbb/build/libs/*.jar app.jar

EXPOSE 10000
ENTRYPOINT ["java","-jar","/app/app.jar"]
