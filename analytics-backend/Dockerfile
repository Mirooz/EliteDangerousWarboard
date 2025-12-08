FROM bellsoft/liberica-openjdk-debian:17 AS build

WORKDIR /app

# Copy ONLY this module, not the whole repo
COPY analytics-backend/ .

RUN chmod +x mvnw
RUN ./mvnw -q -pl analytics-backend -am -DskipTests package

# ---- Runtime ----
FROM bellsoft/liberica-openjre-debian:17
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

CMD ["sh", "-c", "java -Dspring.profiles.active=prod -jar app.jar"]
