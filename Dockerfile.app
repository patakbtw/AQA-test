FROM eclipse-temurin:17-jre

WORKDIR /app

COPY internal-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dsecret=qazWSXedc", "-Dmock=http://wiremock:8888/", "-jar", "app.jar"]
