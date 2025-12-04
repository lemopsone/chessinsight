package ru.chessinsight.infrastructure.cli;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Component
public class CommandLoop implements ApplicationRunner {
    private final ConsoleRouter router;

    public CommandLoop(ConsoleRouter router) {
        this.router = router;
    }

    @Override
    public void run(ApplicationArguments args) {
        router.printHelp();
        try (var reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (true) {
                System.out.print("> ");
                String line = reader.readLine();
                if (line == null) {
                    System.out.println("\nBye!");
                    break;
                }
                try {
                    if (!router.dispatch(line)) break;
                } catch (Throwable t) {
                    CliErrorHandler.handle(t);
                }
            }
        } catch (Throwable t) {
            CliErrorHandler.handle(t);
        }
    }
}
