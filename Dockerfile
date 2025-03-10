# Build stage
FROM gradle:8.5-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Runtime stage
FROM openjdk:21-slim
WORKDIR /app

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Create a non-root user for security
RUN useradd -m -u 1000 appuser && \
    chown -R appuser:appuser /app
USER appuser

# Environment variables
ENV PORT=8080
ENV BOT_STATE_FILE=/app/bot_state.json
ENV JAVA_OPTS="-Dlogback.configurationFile=/app/logback.xml -Dlogging.level.root=INFO -Dlogging.level.chat.hitown.bot=DEBUG -Dbot.secret=${BOT_SECRET}"

# Create log directory with proper permissions
RUN mkdir -p /app/logs && \
    chown -R appuser:appuser /app/logs

# Expose the port the app runs on
EXPOSE 8080

# Run the application with debug logging
ENTRYPOINT ["sh", "-c", "echo 'Starting bot with BOT_SECRET=${BOT_SECRET}' && java $JAVA_OPTS -jar app.jar"]
