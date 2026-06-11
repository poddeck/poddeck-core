FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

COPY build/libs/core-*.jar core.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

# Upgrade OpenSSL to patch CVE-2026-45447 (fixed in 3.5.7-r0); base image
# still ships 3.5.6-r0. Drop once eclipse-temurin:25-jre-alpine is rebuilt.
RUN apk add --no-cache curl && \
    apk upgrade --no-cache libcrypto3 libssl3 openssl && \
    chmod +x /app/docker-entrypoint.sh && \
    chown -R 1000:1000 /app

USER 1000

EXPOSE 8080 10101

HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://127.0.0.1:8080/v1/ || exit 1

ENTRYPOINT ["/app/docker-entrypoint.sh"]
