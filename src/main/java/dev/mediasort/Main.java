// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.io.PrintStream;
import java.util.Arrays;

/**
 * Entry point — parses CLI args then delegates to {@link MediaSort}.
 */
public class Main {

    public static void main(String[] args) {
        // Force UTF-8 output on Windows consoles
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
            System.setErr(new PrintStream(System.err, true, "UTF-8"));
        } catch (Exception ignored) {}

        if (Arrays.asList(args).contains("--install")) {
            try {
                new Installer().install();
            } catch (Exception e) {
                System.err.println("Install failed: " + e.getMessage());
                System.exit(1);
            }
            return;
        }

        CliArgs cliArgs = CliArgs.parse(args);
        try {
            new MediaSort(cliArgs).run();
        } catch (Exception e) {
            System.err.println("Erreur fatale : " + e.getMessage());
            System.exit(1);
        }
    }
}
