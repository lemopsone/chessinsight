package ru.chessinsight.infrastructure.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    private final MoveAnalysisAdmissionInterceptor moveAnalysisAdmissionInterceptor;

    public WebMvcConfig(MoveAnalysisAdmissionInterceptor moveAnalysisAdmissionInterceptor) {
        this.moveAnalysisAdmissionInterceptor = moveAnalysisAdmissionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(moveAnalysisAdmissionInterceptor)
                .addPathPatterns("/v1/move-evaluations");
    }
}
