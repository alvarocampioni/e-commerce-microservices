FROM openjdk:23
COPY ./target/user-service-0.0.1-SNAPSHOT.jar /app/user-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "user-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8000