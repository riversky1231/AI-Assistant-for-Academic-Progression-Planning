FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace/backend
COPY backend/pom.xml ./pom.xml
COPY backend/src ./src
COPY backend/skills ./skills
COPY sql /workspace/sql
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp package

FROM eclipse-temurin:17-jre-jammy AS runtime
RUN curl --version \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build --chown=app:app /workspace/backend/target/academic-planning-backend-1.0.0.jar ./app.jar
ENV SERVER_PORT=8080 \
    SPRING_SQL_INIT_MODE=never \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8"
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=60s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
