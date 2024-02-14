FROM docker.io/maven:3.9.4-amazoncorretto-21 as builder

WORKDIR /app

COPY . .

RUN mvn package

FROM docker.io/amazoncorretto:21

WORKDIR /app

COPY --from=builder /app/target/jpdfsigner-1.0-SNAPSHOT.jar /app

CMD [ "java", "-jar", "jpdfsigner-1.0-SNAPSHOT.jar" ]
