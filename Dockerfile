# Two stages: stage 1 splits the jar built by CI (or locally via
# ./gradlew bootJar) into layers, so the large dependencies layer only changes
# when dependencies change. The Docker build does not run Gradle.
FROM eclipse-temurin:25-jre AS extract
WORKDIR /app
COPY build/libs/starfare.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

FROM eclipse-temurin:25-jre
# curl for the Coolify health check, which runs via `docker exec` inside the
# container; the Temurin JRE image ships no HTTP client.
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=extract /app/extracted/dependencies/ ./
COPY --from=extract /app/extracted/spring-boot-loader/ ./
COPY --from=extract /app/extracted/snapshot-dependencies/ ./
COPY --from=extract /app/extracted/application/ ./
# Round deadlines use wall-clock time; storage stays UTC (hibernate.jdbc.time_zone).
ENV TZ=Europe/Berlin
# No fixed -Xmx: the container memory limit (Coolify) drives the heap size.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
USER 1000:1000
EXPOSE 8080
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
