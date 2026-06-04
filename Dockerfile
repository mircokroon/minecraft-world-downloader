# syntax=docker/dockerfile:1
# Minecraft World Downloader + web management console.

# ---- Stage 1: build the downloader jar ----
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -q -B clean package -DskipTests

# ---- Stage 2: runtime (JRE for the downloader + Python for the web console) ----
FROM eclipse-temurin:21-jre

ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update \
 && apt-get install -y --no-install-recommends python3 python3-pip \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /build/target/world-downloader.jar /app/world-downloader.jar
COPY web /app/web
RUN pip3 install --no-cache-dir --break-system-packages -r /app/web/requirements.txt

ENV JAR_PATH=/app/world-downloader.jar \
    DATA_DIR=/data \
    WEB_PORT=8080

RUN mkdir -p /data
VOLUME ["/data"]

# 8080 = web console, 25565 = the Minecraft proxy clients connect to
EXPOSE 8080 25565

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD python3 -c "import urllib.request,sys; sys.exit(0 if urllib.request.urlopen('http://127.0.0.1:8080/healthz').status==200 else 1)" || exit 1

ENTRYPOINT ["python3", "/app/web/app.py"]
