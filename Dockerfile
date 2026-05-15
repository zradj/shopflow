# ─────────────────────────────────────────────────────────────────
#  ShopFlow — Multi-stage Dockerfile
#
#  Stage 1 (builder): compiles and packages the fat JAR with Maven
#  Stage 2 (runtime): copies only the JAR into a slim JRE image
#
#  Result: a lean production image (~300 MB) with no Maven or source
#  code included.
# ─────────────────────────────────────────────────────────────────

# ── Stage 1: Build ───────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

# Copy dependency descriptors first so Docker can cache the
# dependency-download layer independently of source changes.
COPY pom.xml .
COPY .mvn/ .mvn/

# Download all dependencies (cached unless pom.xml changes)
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -B --quiet

# Copy source and build the fat JAR, skipping tests
# (tests require Redis which is not available at image-build time)
COPY src/ src/
RUN mvn package -DskipTests -B --quiet

# Stage 2: Runtime
FROM eclipse-temurin:21-jdk-alpine AS runtime

RUN addgroup -S shopflow && adduser -S shopflow -G shopflow
USER shopflow

WORKDIR /app
COPY --from=builder /build/target/shopflow-*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -Djdk.tracePinnedThreads=short"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
