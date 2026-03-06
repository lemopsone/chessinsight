package ru.chessinsight.infrastructure.cli;

import org.springframework.stereotype.Component;
import ru.chessinsight.infrastructure.cli.controller.CommandController;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ConsoleRouter {
    private final Map<String, CommandController> controllers = new HashMap<>();

    public ConsoleRouter(List<CommandController> controllers) {
        controllers.forEach(this::register);
    }

    public void register(CommandController controller) {
        controllers.put(controller.name(), controller);
    }

    public boolean dispatch(String input) {
        String[] parts = input.trim().split("\\s+");
        if (parts.length == 0) return true;
        if ("exit".equalsIgnoreCase(parts[0])) return false;

        CommandController ctrl = controllers.get(parts[0]);
        if (ctrl == null) {
            System.out.println("Unknown command: " + parts[0]);
            return true;
        }
        ctrl.handle(Arrays.copyOfRange(parts, 1, parts.length));
        return true;
    }

    public void printHelp() {
        System.out.println("Available commands:");
        controllers.values().forEach(c ->
                System.out.printf(" - %s : %s%n", c.name(), c.description()));
        System.out.println("Type 'exit' to quit.");
    }
}
