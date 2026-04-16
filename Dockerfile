# ── Build stage ────────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:resolve -q || true
COPY src ./src
RUN mvn package -DskipTests -q

# ── Runtime stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-focal
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends \
        xvfb \
        x11vnc \
        novnc \
        websockify \
        libxext6 \
        libxrender1 \
        libxtst6 \
        libxi6 \
        libxfixes3 \
        fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/IPOS-CA-1.0-SNAPSHOT.jar app.jar
COPY sql/ ./sql/
COPY database/ ./database/
COPY start.sh .
RUN chmod +x start.sh

EXPOSE 5900 6080 8082

ENTRYPOINT ["./start.sh"]
