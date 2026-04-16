# MarryLink Backend Dockerfile
# 直接部署本地打包好的 jar 包

# 使用华为云镜像加速的 OpenJDK 17
FROM  eclipse-temurin:17-jre-alpine

LABEL maintainer="MarryLink Team"
LABEL description="MarryLink Backend Service"

WORKDIR /app

# 设置时区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 复制 jar 文件（构建前请先将 jar 包复制到此目录）
# 方式1: cp /root/*.jar /path/to/marrylink-admin/backend/
# 方式2: 如果 jar 在 target 目录: mvn clean package -DskipTests
COPY *.jar app.jar

# 创建日志目录
RUN mkdir -p /app/logs

# 暴露端口（8080为应用端口，5005为远程调试端口）
EXPOSE 8080 5005

# JVM 优化参数（默认值，可通过 docker-compose 环境变量覆盖）
# 注意：不在此处设置 ENV JAVA_OPTS，让 docker-compose.yml 中的环境变量生效

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/api/v1/actuator/health || exit 1

# 启动命令（指定 docker profile，使用 application-docker.yml 中的环境变量配置）
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=docker -jar app.jar"]