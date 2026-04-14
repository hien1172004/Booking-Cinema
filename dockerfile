FROM eclipse-temurin:21-jre-jammy

ENV TZ=Asia/Ho_Chi_Minh
ENV JAVA_TOOL_OPTIONS="-Duser.timezone=Asia/Ho_Chi_Minh"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

WORKDIR /app

COPY target/*.jar app.jar

RUN useradd -m appuser && \
    mkdir -p /app/logs && \
    chown -R appuser:appuser /app

USER appuser

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]