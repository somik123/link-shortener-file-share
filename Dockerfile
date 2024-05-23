# Build application
FROM somik123/ubuntu:22-jdk21-mvn396 as builder

# Finally start building spring boot app
WORKDIR /app
COPY src src
COPY pom.xml .

RUN mvn -f ./pom.xml clean package -Dmaven.test.skip=true
# End build


# Run application
# FROM somik123/ubuntu:22-jdk21
FROM eclipse-temurin:21.0.2_13-jre-alpine

WORKDIR /usr/app

COPY uploads uploads
COPY tmp_uploads tmp_uploads
COPY logs logs
COPY --from=builder /app/target/shorturl-*.jar shorturl-fileshare.jar

EXPOSE 8080

# start app
CMD ["java", "-jar", "shorturl-fileshare.jar"]