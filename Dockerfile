FROM maven:3-eclipse-temurin-8 AS build

# Build dependencies
COPY pom.xml .
WORKDIR /build
COPY filters/pom.xml .
RUN mvn dependency:go-offline -B

# Build release
COPY filters/src src 
RUN mvn clean package -DskipTests

RUN mkdir release && \
    cp target/filters-*.jar release/filters.jar && \
    cp src/main/resources/config.sample.properties release/config.properties

# Final image
FROM amazoncorretto:8-alpine-jre

COPY --from=build /build/release /app
WORKDIR /app
CMD ["java", "-cp", ".:filters.jar", "com.matecat.converter.Main"]
