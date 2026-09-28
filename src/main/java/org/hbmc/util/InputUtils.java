package org.hbmc.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.Predicate;

public class InputUtils {

    private final Scanner scanner;

    public InputUtils(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        return this.scanner.nextLine().trim();
    }

    public String readNonEmptyLine(String prompt) {
        return this.readValid(prompt, input -> !input.isBlank(), "Input cannot be empty. Try again.");
    }

    public int readInt(String prompt) {
        return this.readParsed(prompt, Integer::parseInt, "Please enter a valid whole number.");
    }

    public BigDecimal readBigDecimal(String prompt) {
        return this.readParsed(prompt, BigDecimal::new, "Please enter a valid amount.");
    }

    public LocalDate readDate(String prompt) {
        return this.readParsed(prompt, LocalDate::parse, "Please enter a valid date (format: YYYY-MM-DD).");
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String input = this.readLine(prompt + " (y/n): ").toLowerCase();
            if (input.equals("y") || input.equals("yes")) return true;
            if (input.equals("n") || input.equals("no")) return false;
            System.out.println("Please answer y or n.");
        }
    }

    private String readValid(String prompt, Predicate<String> isValid, String errorMessage) {
        while (true) {
            String input = this.readLine(prompt);
            if (isValid.test(input)) {
                return input;
            }
            System.out.println(errorMessage);
        }
    }

    private <T> T readParsed(String prompt, Function<String, T> parser, String errorMessage) {
        while (true) {
            String input = this.readLine(prompt);
            try {
                return parser.apply(input);
            } catch (NumberFormatException | DateTimeParseException e) {
                System.out.println(errorMessage);
            }
        }
    }
}
