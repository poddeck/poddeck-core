FROM eclipse-temurin:25-jdk AS build

WORKDIR /build

COPY gradle/ gradle/
COPY gradlew settings.gradle.kts build.gradle.kts ./
RUN chmod +x gradlew

COPY src/ src/

RUN --mount=type=secret,id=GITHUB_USERNAME \
    --mount=type=secret,id=GITHUB_TOKEN \
    GITHUB_USERNAME=$(cat /run/secrets/GITHUB_USERNAME) \
    GITHUB_TOKEN=$(cat /run/secrets/GITHUB_TOKEN) \
    ./gradlew bootJar -x test

FROM eclipse-temurin:25-jre

WORKDIR /app

COPY --from=build /build/build/libs/core-1.0.0-SNAPSHOT.jar core.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN apt-get update && apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/* && \
    chmod +x /app/docker-entrypoint.sh && \
    chown -R 1000:1000 /app

USER 1000

EXPOSE 8080 10101

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/ || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
