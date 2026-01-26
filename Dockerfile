FROM eclipse-temurin:24-jdk AS build

RUN apt-get update && apt-get install -y wget git build-essential cmake \
    && wget https://downloads.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz \
    && tar -xzf apache-maven-3.9.11-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-3.9.11 /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/bin/mvn \
    && rm apache-maven-3.9.11-bin.tar.gz

WORKDIR /build
COPY pom.xml .
COPY core/pom.xml core/
COPY cli/pom.xml cli/
COPY engine/pom.xml engine/
COPY jpa/pom.xml jpa/
COPY web/pom.xml web/

RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -pl web,cli -am -DskipTests \
    && mkdir -p /build/static/downloads \
    && cp /build/cli/target/cli-1.0.0.jar /build/static/downloads/chessinsight-cli.jar

FROM nginx:1.27-alpine AS gateway
RUN apk add --no-cache curl unzip \
    && curl -fsSL https://releases.hashicorp.com/consul-template/0.37.4/consul-template_0.37.4_linux_amd64.zip -o /tmp/consul-template.zip \
    && unzip /tmp/consul-template.zip -d /usr/local/bin \
    && rm /tmp/consul-template.zip \
    && chmod +x /usr/local/bin/consul-template
COPY nginx/nginx.conf /etc/nginx/nginx.conf
COPY nginx/conf.d /etc/nginx/conf.d
COPY nginx/docker-entrypoint.d /docker-entrypoint.d
COPY --from=build /build/static /var/www/static

FROM nginx:1.27-alpine AS stockfish-pool
RUN apk add --no-cache curl unzip \
    && curl -fsSL https://releases.hashicorp.com/consul-template/0.37.4/consul-template_0.37.4_linux_amd64.zip -o /tmp/consul-template.zip \
    && unzip /tmp/consul-template.zip -d /usr/local/bin \
    && rm /tmp/consul-template.zip \
    && chmod +x /usr/local/bin/consul-template
COPY nginx/stockfish-stream.conf /etc/nginx/nginx.conf
COPY nginx/stockfish-entrypoint.d /docker-entrypoint.d
RUN chmod +x /docker-entrypoint.d/*.sh

FROM eclipse-temurin:24-jre AS runtime

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=4 -XX:+UseG1GC"

RUN apt-get update && apt-get install -y \
    libprotobuf-dev libopenblas-dev libomp-dev && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/web/target/web-1.0.0.jar /app/app.jar
COPY testData /test

WORKDIR /app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
