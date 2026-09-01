# Spring Boot backend.
# Build context is the REPOSITORY ROOT so the single root .dockerignore applies
# (Docker reads .dockerignore from the context root, not from the repo root).
# Build:  docker build -f docker/backend.Dockerfile -t nia-backend .
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY backend/pom.xml .
RUN mvn -q -e -B dependency:go-offline
COPY backend/src ./src
RUN mvn -q -B -DskipTests package

# Alpine JRE: ~170 MB smaller than the Debian-based tag, which matters for cold
# starts on a free hosting tier. Verified on this image, not assumed: DB pool,
# JWT auth, DB writes, native queries, and outbound HTTPS via Netty/WebClient
# (a live news fetch returned real articles) all work under musl libc.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Run as an unprivileged user: a process that only needs to read a jar and open
# port 8080 has no reason to be root inside the container.
RUN addgroup -S nia \
    && adduser -S -u 10001 -H -D -s /sbin/nologin -G nia nia \
    && chown nia:nia /app/app.jar
USER nia

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
