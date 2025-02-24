FROM openjdk:23
COPY ./target/email-service-0.0.1-SNAPSHOT.jar /app/email-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "email-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8085
