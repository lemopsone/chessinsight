package ru.chessinsight.infrastructure.logger;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.chessinsight.application.common.logger.service.Logger;

@Component
public class Slf4jLogger implements Logger {
    private final org.slf4j.Logger logger;

    public Slf4jLogger() {
        this.logger = LoggerFactory.getLogger("ru.chessinsight.application");
    }

    @Override
    public void info(String message) {
        logger.info(message);
    }

    @Override
    public void warning(String message) {
        logger.warn(message);
    }

    @Override
    public void error(String message) {
        logger.error(message);
    }
}
