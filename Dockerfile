FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="ArmorAuth Team"

WORKDIR /app

COPY armorauth-server/target/armorauth-server-*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
