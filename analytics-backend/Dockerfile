FROM bellsoft/liberica-openjdk-debian:17 as build

WORKDIR /app
COPY . .

# Fix Render: rendre le wrapper Maven exécutable
RUN chmod +x mvnw

# Build
RUN ./mvnw -q -DskipTests package

# ---- Runtime ----
FROM bellsoft/liberica-openjre-debian:17

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

CMD ["sh", "-c", "java -Dspring.profiles.active=prod -jar app.jar"]
