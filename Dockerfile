FROM openjdk:26-ea-jdk-slim
LABEL maintainer="appupiratla@gmail.com"
LABEL version="1.0"
LABEL description=""
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]