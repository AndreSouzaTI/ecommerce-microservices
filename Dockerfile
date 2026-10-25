# Imagem única para todos os serviços: escolha o módulo com --build-arg SERVICE=<nome>
FROM eclipse-temurin:21-jdk-alpine AS build
ARG SERVICE
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY discovery-server/pom.xml discovery-server/
COPY api-gateway/pom.xml api-gateway/
COPY catalog-service/pom.xml catalog-service/
COPY order-service/pom.xml order-service/
RUN chmod +x mvnw && ./mvnw -B -q -pl ${SERVICE} -am dependency:go-offline
COPY ${SERVICE}/src ${SERVICE}/src
RUN ./mvnw -B -q -pl ${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
ARG SERVICE
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /app/${SERVICE}/target/app.jar app.jar
USER app
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
