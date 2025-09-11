package ru.chessinsight;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@SpringBootApplication
public class ChessInsight {
    public static void main(String[] args) {
        SpringApplication.run(ChessInsight.class, args);
    }
}