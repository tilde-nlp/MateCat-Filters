# BUILD IMAGE
FROM maven:3.8-openjdk-8-slim as maven

# copy the root parent-pom
COPY pom.xml .

# set directory
WORKDIR /build

# copy the filters project pom to workdir
COPY filters/pom.xml /build

# build all dependencies
RUN mvn dependency:go-offline -B

# copy the source tree
COPY filters/src /build/src    

# build for release
RUN mvn clean package -DskipTests


# DEPLOY IMAGE
# Use a slimmer deploy image with fewer layers
FROM openjdk:8-alpine

# copy over the config file from the maven image
COPY --from=maven /build/src/main/resources/config.sample.properties config.properties

# copy over the built artifact from the maven image
COPY --from=maven /build/target/filters-*.jar app.jar

# Expose port
EXPOSE 8732

# set the startup command to run your binary
CMD ["java", "-cp", ".:app.jar", "com.matecat.converter.Main"]