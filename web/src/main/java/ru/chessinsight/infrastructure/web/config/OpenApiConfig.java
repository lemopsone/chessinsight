package ru.chessinsight.infrastructure.web.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenApiCustomizer removeDefaultResponses() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().values().forEach(path ->
                    path.readOperations().forEach(op -> {
                        if (op.getResponses() != null) {
                            op.getResponses().remove("default");
                        }
                    }));
        };
    }
}
