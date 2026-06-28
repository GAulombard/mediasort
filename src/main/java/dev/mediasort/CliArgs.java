// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.nio.file.Path;

/**
 * Immutable record holding all validated CLI arguments.
 */
public record CliArgs(
        Path source,
        Path destination,
        boolean move,
        boolean noMonth,
        boolean dryRun,
        int threads,
        boolean verbose,
        boolean rebuild
) {

    /** Parses raw CLI args and returns a validated {@link CliArgs}. */
    public static CliArgs parse(String[] args) {
        if (args.length == 0 || hasFlag(args, "--help")) {
            printHelp();
            System.exit(0);
        }

        if (args.length < 2) {
            System.err.println("Error: <source> and <destination> are required.");
            System.err.println("Run with --help for usage.");
            System.exit(1);
        }

        Path source = Path.of(args[0]);
        Path destination = Path.of(args[1]);

        if (!source.toFile().isDirectory()) {
            System.err.println("Error: source directory does not exist: " + source);
            System.exit(1);
        }

        boolean move = hasFlag(args, "--move");
        boolean noMonth = hasFlag(args, "--no-month");
        boolean dryRun = hasFlag(args, "--dry-run");
        boolean verbose = hasFlag(args, "--verbose");
        boolean rebuild = hasFlag(args, "--rebuild");
        int threads = parseThreads(args);

        return new CliArgs(source, destination, move, noMonth, dryRun, threads, verbose, rebuild);
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) return true;
        }
        return false;
    }

    private static int parseThreads(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--threads=")) {
                try {
                    int n = Integer.parseInt(arg.substring("--threads=".length()));
                    return Math.max(1, n);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid value for --threads, defaulting to 4.");
                }
            }
        }
        return 4;
    }

    private static void printHelp() {
        System.out.println("""
                Usage: mediasort <source> <destination> [options]

                Automatically sorts photos and videos by year and month.

                Positional arguments:
                  source        Directory containing the files to sort
                  destination   Destination directory (created if missing)

                Options:
                  --move         Move instead of copy (default: copy)
                  --no-month     Sort by year only, no month subdirectory
                  --dry-run      Simulate, no files are written
                  --threads=N    Number of parallel threads (default: 4)
                  --verbose      Detailed output
                  --rebuild      Force JAR recompilation
                  --help         Show this help

                Examples:
                  mediasort /photos /backup/photos --dry-run
                  mediasort C:\\Photos D:\\Backup --move
                  mediasort /data/photos /sorted --verbose --threads=8
                """);
    }
}
