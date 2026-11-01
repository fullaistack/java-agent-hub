# =====================================================================
# java-agent-hub Dockerfile
# 构建：mvn clean package -DskipTests 后执行 docker build -t agenthub:1.0.0 .
# =====================================================================
FROM eclipse-temurin:17-jre

# 时区与字体（Tika 解析 PDF 需要字体支持）
RUN apt-get update && apt-get install -y --no-install-recommends \
        fontconfig fonts-noto-cjk tzdata \
    && ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# 可执行 jar（spring-boot-maven-plugin 已打包为 agenthub.jar）
COPY target/agenthub.jar app.jar

# OTel Java Agent 可选挂载（开启自动埋点）
# COPY opentelemetry-javaagent.jar /app/otel/

EXPOSE 8080

ENV JAVA_OPTS="-Xms512m -Xmx1g"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
