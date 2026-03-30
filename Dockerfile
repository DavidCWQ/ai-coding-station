# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy

ARG TARGETARCH
ENV NODE_VERSION=24.12.0

RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl xz-utils \
    && NODE_ARCH=$([ "$TARGETARCH" = "arm64" ] && echo arm64 || echo x64) \
    && curl -fsSL "https://nodejs.org/dist/v${NODE_VERSION}/node-v${NODE_VERSION}-linux-${NODE_ARCH}.tar.xz" \
        | tar -xJ -C /usr/local --strip-components=1 \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /build/target/*.jar /app/app.jar
COPY scripts/package.json scripts/package-lock.json scripts/screenshot.js /app/scripts/

WORKDIR /app/scripts
RUN npm ci \
    && npx playwright install-deps chromium \
    && npx playwright install chromium

WORKDIR /app

EXPOSE 8142

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
