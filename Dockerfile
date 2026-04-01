# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build

# 先拷贝依赖描述文件，利用 Docker 层缓存
COPY pom.xml .
COPY src ./src

# 使用 BuildKit 缓存 Maven 本地仓库，避免每次都重新下载依赖
RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -DskipTests package

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

COPY scripts/package.json scripts/package-lock.json scripts/screenshot.js /app/scripts/

# 预置 Playwright 浏览器缓存：
# 如果存在 docker/ms-playwright/，会被复制到 /root/.cache/ms-playwright，
# 配合下方 BuildKit 缓存，可避免每次构建都重新下载浏览器。
COPY docker/ms-playwright /root/.cache/ms-playwright

WORKDIR /app/scripts
# Playwright 浏览器缓存：
# 需开启 BuildKit 缓存挂载，Docker 24+ 默认开启；
# 同一台构建机会复用 /root/.cache/ms-playwright，避免每次重下 chrome-linux64.zip。
RUN --mount=type=cache,target=/root/.cache/ms-playwright,id=playwright-browsers \
    npm ci \
    && npx playwright install-deps chromium \
    && npx playwright install chromium

WORKDIR /app
COPY --from=build /build/target/*.jar /app/app.jar

EXPOSE 8142

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
