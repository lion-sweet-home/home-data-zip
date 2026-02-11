# 1. 빌드 스테이지
FROM gradle:8-jdk21 AS build
WORKDIR /app

# 소스 코드 복사
COPY . .
RUN chmod +x gradlew
RUN ./gradlew clean build -x test

# 2. 실행 스테이지
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 애플리케이션 실행
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]