# ============================================
# STAGE 1: BUILD
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copia só o pom primeiro (cache de dependências)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copia o código e builda
COPY src ./src
RUN mvn clean package -DskipTests -B

# ============================================
# STAGE 2: RUNTIME
# ============================================
FROM eclipse-temurin:17-jre-alpine AS runtime

# Instala wget pra healthcheck
RUN apk add --no-cache wget

# Cria usuário não-root
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

# Copia o jar buildado
COPY --from=build /app/target/*.jar app.jar

# Muda pro usuário não-root
USER app

EXPOSE 8080

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# JVM otimizada pra container
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]