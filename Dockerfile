FROM eclipse-temurin:21-jre-alpine

# 安装 Docker 客户端（用于 Docker-in-Docker）
RUN apk add --no-cache docker-cli

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
