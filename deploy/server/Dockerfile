FROM eclipse-temurin:17-jre-jammy
RUN curl --version \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --chown=app:app app.jar ./app.jar
ENV SERVER_PORT=8080 SPRING_SQL_INIT_MODE=never
USER app
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=5 \
    CMD curl --fail --silent --show-error http://127.0.0.1:8080/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
