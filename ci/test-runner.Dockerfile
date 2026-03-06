FROM eclipse-temurin:24-jdk

RUN apt-get update && apt-get install -y git maven curl unzip jq python3 && rm -rf /var/lib/apt/lists/*

ARG ALLURE_VERSION=2.27.0
RUN curl -Ls https://github.com/allure-framework/allure2/releases/download/${ALLURE_VERSION}/allure-${ALLURE_VERSION}.zip -o /tmp/allure.zip \
    && unzip /tmp/allure.zip -d /opt \
    && ln -s /opt/allure-${ALLURE_VERSION}/bin/allure /usr/local/bin/allure \
    && rm /tmp/allure.zip

COPY ci/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
