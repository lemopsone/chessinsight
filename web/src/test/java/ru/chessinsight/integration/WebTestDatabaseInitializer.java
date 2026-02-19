package ru.chessinsight.integration;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.DriverManager;
import java.util.Locale;
import java.util.UUID;

public class WebTestDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        String mode = value("test.db.mode", "TEST_DB_MODE", "testcontainers");
        if (!"local-shared".equalsIgnoreCase(mode)) {
            return;
        }

        String baseUrl = required("test.db.url", "TEST_DB_URL");
        String username = value("test.db.username", "TEST_DB_USERNAME", "postgres");
        String password = value("test.db.password", "TEST_DB_PASSWORD", "postgres");
        String prefix = value("test.db.schema.prefix", "TEST_DB_SCHEMA_PREFIX", "e2e");
        String schemaName = value("test.db.schema.name", "TEST_DB_SCHEMA_NAME", "");
        String schema = schemaName.isBlank()
                ? sanitizeSchema(prefix + "_" + UUID.randomUUID().toString().replace("-", ""))
                : sanitizeSchema(schemaName);
        String url = withCurrentSchema(baseUrl, schema);

        TestPropertyValues.of(
                "spring.datasource.url=" + url,
                "spring.datasource.username=" + username,
                "spring.datasource.password=" + password,
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.hikari.connection-init-sql=CREATE SCHEMA IF NOT EXISTS " + schema,
                "spring.jpa.properties.hibernate.default_schema=" + schema,
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema.sql",
                "spring.jpa.hibernate.ddl-auto=none"
        ).applyTo(applicationContext.getEnvironment());

        Runtime.getRuntime().addShutdownHook(new Thread(() -> dropSchema(baseUrl, username, password, schema)));
    }

    private static String value(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue;
        }
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue;
        }
        return defaultValue;
    }

    private static String required(String propertyKey, String envKey) {
        String value = value(propertyKey, envKey, "");
        if (value.isBlank()) {
            throw new IllegalStateException(propertyKey + " (or " + envKey + ") must be set for local-shared mode");
        }
        return value;
    }

    private static String sanitizeSchema(String schema) {
        String normalized = schema.toLowerCase(Locale.ROOT);
        if (!normalized.matches("[a-z0-9_]+")) {
            throw new IllegalStateException("Invalid schema name: " + schema);
        }
        return normalized;
    }

    private static String withCurrentSchema(String baseUrl, String schema) {
        if (baseUrl.contains("?")) {
            return baseUrl + "&currentSchema=" + schema;
        }
        return baseUrl + "?currentSchema=" + schema;
    }

    private static void dropSchema(String baseUrl, String username, String password, String schema) {
        try (var connection = DriverManager.getConnection(baseUrl, username, password);
             var statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + schema + " CASCADE");
        } catch (Exception ignored) {
        }
    }
}
