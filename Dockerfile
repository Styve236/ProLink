# ============================================
# Build : compilation avec Maven (JDK 25)
# ============================================
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Cache des dépendances Maven
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -B dependency:go-offline || true

# Code source + compilation
COPY src src
RUN ./mvnw -B package -DskipTests

# ============================================
# Runtime : image Java minimale
# ============================================
FROM eclipse-temurin:25-jdk
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=render

# Dossier d'upload persistant (monté via le disque Render)
RUN mkdir -p /data/uploads

# Copie du jar construit
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
