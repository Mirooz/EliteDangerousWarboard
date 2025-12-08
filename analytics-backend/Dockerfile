# ---- Étape de build : Liberica JDK ----
FROM bellsoft/liberica-openjdk-debian:17 AS build

WORKDIR /app
COPY . .

RUN ./mvnw -q -DskipTests package

# ---- Étape runtime : Liberica JRE ----
FROM bellsoft/liberica-openjre-debian:17

WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-Dspring.profiles.active=prod"

CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
