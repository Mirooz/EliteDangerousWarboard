FROM bellsoft/liberica-openjdk-debian:17 AS build

WORKDIR /app

# Copy ONLY what Maven needs
COPY pom.xml .
COPY analytics-backend ./analytics-backend

# Copy Maven wrapper
COPY mvnw .
COPY .mvn ./.mvn

RUN chmod +x mvnw

# Build only your module
RUN ./mvnw -q -pl analytics-backend -am -DskipTests package

# ---- Runtime ----
FROM bellsoft/liberica-openjre-debian:17
WORKDIR /app

COPY --from=build /app/analytics-backend/target/*.jar app.jar

CMD ["sh", "-c", "java -Dspring.profiles.active=prod -jar app.jar"]
