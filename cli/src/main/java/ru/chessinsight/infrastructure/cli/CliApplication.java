package ru.chessinsight.infrastructure.cli;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication(
    scanBasePackages = "ru.chessinsight",
    exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
    }
)
public class CliApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(CliApplication.class)
                .web(WebApplicationType.NONE)
                .lazyInitialization(true)
                .run(args);
    }}
