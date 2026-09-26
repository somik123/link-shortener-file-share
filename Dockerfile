# Build application
FROM somik123/ubuntu:26-jdk-mvn AS builder

# Native font libraries required by AWT for image rendering tests (captcha, Text2Image)
RUN apt-get update \
    && DEBIAN_FRONTEND=noninteractive apt-get install -y --no-install-recommends \
       libfreetype6 fontconfig fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Finally start building spring boot app
WORKDIR /app
COPY src src
COPY pom.xml .

RUN mvn -f ./pom.xml clean package # -Dmaven.test.skip=true
# End build


# Run application
FROM eclipse-temurin:26-jre-alpine

WORKDIR /usr/app

COPY data data
COPY --from=builder /app/target/shorturl-*.jar shorturl-fileshare.jar

EXPOSE 8080

# start app
CMD ["java", "-jar", "shorturl-fileshare.jar"]