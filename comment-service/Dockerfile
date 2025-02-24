FROM openjdk:23
COPY ./target/comment-service-0.0.1-SNAPSHOT.jar /app/comment-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "comment-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8083