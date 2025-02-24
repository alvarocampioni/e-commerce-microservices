FROM openjdk:23
COPY ./target/payment-service-0.0.1-SNAPSHOT.jar /app/payment-service-0.0.1-SNAPSHOT.jar
WORKDIR /app
ENTRYPOINT ["java", "-jar", "payment-service-0.0.1-SNAPSHOT.jar"]
EXPOSE 8084