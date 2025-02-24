FROM openjdk:23
COPY ./target/service-discovery-0.0.1-SNAPSHOT.jar /app/service-discovery-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "service-discovery-0.0.1-SNAPSHOT.jar"]
EXPOSE 8888
