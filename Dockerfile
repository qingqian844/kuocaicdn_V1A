FROM maven:3-amazoncorretto-8 AS builder

RUN mkdir -p /app

COPY .mvn /app/.mvn
COPY libs /app/libs
COPY pom.xml /app/pom.xml

WORKDIR /app

RUN mvn initialize
RUN mvn dependency:go-offline

COPY src /app/src

RUN mvn clean package -DskipTests

FROM alibabadragonwell/dragonwell:8-alinux AS runner

RUN mkdir -p /app

COPY --from=builder /app/target/KuocaiCDN-0.0.1-SNAPSHOT.jar /app/kuocaicdn.jar

# RUN apk add --no-cache tzdata

WORKDIR /app

EXPOSE 8000

ENTRYPOINT ["java",  "-Xms8g", "-Xmx16g", "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:+AlwaysPreTouch", "-XX:+UseContainerSupport", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseWisp2", "-jar", "./kuocaicdn.jar", "--spring.profiles.active=prod"]
