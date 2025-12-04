FROM eclipse-temurin:24-jdk as build

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

RUN mvn dependency:go-offline -B

COPY . .
RUN mvn clean package -pl cli -am -DskipTests

#FROM eclipse-temurin:24-jdk as base
#
#RUN apt-get update && apt-get install -y wget \
#    && wget https://downloads.apache.org/maven/maven-3/3.9.11/binaries/apache-maven-3.9.11-bin.tar.gz \
#    && tar -xzf apache-maven-3.9.11-bin.tar.gz -C /opt \
#    && ln -s /opt/apache-maven-3.9.11 /opt/maven \
#    && ln -s /opt/maven/bin/mvn /usr/bin/mvn \
#    && rm apache-maven-3.9.11-bin.tar.gz

#FROM ubuntu:22.04 AS build-stockfish
#
#RUN apt-get update && apt-get install -y \
#    git build-essential cmake wget
#
#RUN git clone https://github.com/official-stockfish/Stockfish.git /stockfish
#RUN cd /stockfish/src && make build ARCH=x86-64

#FROM ubuntu:22.04 as build-lc0
#
#RUN apt-get update && apt-get install -y \
#    git cmake build-essential protobuf-compiler libprotobuf-dev \
#    libopenblas-dev libomp-dev python3-pip ninja-build
#RUN pip3 install meson --user
#RUN git clone https://github.com/LeelaChessZero/lc0.git /lc0
#RUN /lc0/build.sh

#FROM base
#
#RUN apt-get update && apt-get install -y \
#    libprotobuf-dev libopenblas-dev libomp-dev && rm -rf /var/lib/apt/lists/*

#COPY --from=build-stockfish /stockfish/src/stockfish /usr/local/bin/stockfish
#COPY --from=build-lc0 /lc0/build/release/lc0 /usr/local/bin/lc0

#ENV PATH="/usr/local/bin:${PATH}"

#WORKDIR /app
FROM eclipse-temurin:24-jre as runtime

# JVM tuning for containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:ActiveProcessorCount=4 -XX:+UseG1GC"

RUN apt-get update && apt-get install -y \
    libprotobuf-dev libopenblas-dev libomp-dev && rm -rf /var/lib/apt/lists/*

#COPY --from=build-stockfish /stockfish/src/stockfish /usr/local/bin/stockfish
COPY --from=build /build/cli/target/cli-1.0.0.jar /app/app.jar
COPY testData /test

WORKDIR /app
#ENTRYPOINT ["java", "-jar", "/app/app.jar"]
ENTRYPOINT ["tail", "-f", "/dev/null"]