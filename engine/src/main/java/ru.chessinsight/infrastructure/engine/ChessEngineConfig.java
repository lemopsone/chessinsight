package ru.chessinsight.infrastructure.engine;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.chessinsight.application.game.analysis.engine.ChessEngine;

@Configuration
public class ChessEngineConfig {

    @Bean
    @ConditionalOnProperty(name = "engine.stockfish.mode", havingValue = "local", matchIfMissing = true)
    public ChessEngine localStockfish(
            @Value("${engine.stockfish.path}") String path,
            @Value("${engine.stockfish.defaultDepth:15}") int defaultDepth
    ) throws Exception {
        return new Stockfish(path, defaultDepth);
    }

    @Bean
    @ConditionalOnProperty(name = "engine.stockfish.mode", havingValue = "tcp")
    public ChessEngine tcpStockfish(
            @Value("${engine.stockfish.host}") String host,
            @Value("${engine.stockfish.port}") int port,
            @Value("${engine.stockfish.defaultDepth:15}") int defaultDepth
    ) throws Exception {
        return new TcpStockfish(host, port, defaultDepth);
    }
}
