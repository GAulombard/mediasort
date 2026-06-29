// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
        boolean rebuild,
        List<String> excludePatterns,
        boolean deleteExcluded
) {

    /** Parses raw CLI args and returns a validated {@link CliArgs}. */
    public static CliArgs parse(String[] args) {
        if (args.length == 0 || hasFlag(args, "--help")) {
            printHelp();
            System.exit(0);
        }

        List<String> positionals = new ArrayList<>();
        List<String> excludePatterns = new ArrayList<>();
        boolean move = false, noMonth = false, dryRun = false,
                verbose = false, rebuild = false, deleteExcluded = false;
        int threads = 4;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--move"            -> move = true;
                case "--no-month"        -> noMonth = true;
                case "--dry-run"         -> dryRun = true;
                case "--verbose"         -> verbose = true;
                case "--rebuild"         -> rebuild = true;
                case "--delete-excluded" -> deleteExcluded = true;
                case "--exclude-pattern" -> {
                    if (i + 1 < args.length) {
                        excludePatterns.add(args[++i]);
                    } else {
                        System.err.println("Error: --exclude-pattern requires a value.");
                        System.exit(1);
                    }
                }
                default -> {
                    if (args[i].startsWith("--threads=")) {
                        try {
                            threads = Math.max(1, Integer.parseInt(args[i].substring("--threads=".length())));
                        } catch (NumberFormatException e) {
                            System.err.println("Invalid value for --threads, defaulting to 4.");
                        }
                    } else if (!args[i].startsWith("--")) {
                        positionals.add(args[i]);
                    }
                }
            }
        }

        if (positionals.isEmpty()) {
            System.err.println("Error: <source> is required.");
            System.err.println("Run with --help for usage.");
            System.exit(1);
        }

        Path source = Path.of(positionals.get(0));
        if (!source.toFile().isDirectory()) {
            System.err.println("Error: source directory does not exist: " + source);
            System.exit(1);
        }

        // destination is optional: defaults to <source>/mediasort/
        Path destination = positionals.size() >= 2
                ? Path.of(positionals.get(1))
                : source.resolve("mediasort");

        return new CliArgs(source, destination, move, noMonth, dryRun, threads, verbose, rebuild,
                List.copyOf(excludePatterns), deleteExcluded);
    }

    private static boolean hasFlag(String[] args, String flag) {
        for (String arg : args) {
            if (arg.equals(flag)) return true;
        }
        return false;
    }

    private static void printHelp() {
        System.out.println("""
                Usage: mediasort <source> [destination] [options]

                Automatically sorts photos and videos by year and month.

                Positional arguments:
                  source        Directory containing the files to sort
                  destination   Destination directory (default: <source>/mediasort/)

                Options:
                  --move                   Move instead of copy (default: copy)
                  --no-month               Sort by year only, no month subdirectory
                  --dry-run                Simulate, no files are written
                  --threads=N              Number of parallel threads (default: 4)
                  --verbose                Detailed output
                  --exclude-pattern <pat>  Skip files whose name contains <pat> (repeatable)
                  --delete-excluded        Delete matched files from source instead of skipping
                  --install                Install mediasort globally (~/.mediasort/bin)
                  --rebuild                Force JAR recompilation
                  --help                   Show this help

                Examples:
                  mediasort /photos --dry-run
                  mediasort /photos /backup/photos
                  mediasort C:\\Photos D:\\Backup --move
                  mediasort /photos --exclude-pattern screenshot
                  mediasort /photos --exclude-pattern screenshot --delete-excluded
                  mediasort /data/photos /sorted --verbose --threads=8
                """);
    }
}
