// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Copies or moves a single media file to its dated destination, handling duplicates.
 */
public class FileProcessor {

    private static final String[] MONTH_NAMES = {
            "january", "february", "march", "april", "may", "june",
            "july", "august", "september", "october", "november", "december"
    };

    /**
     * Processes one file: resolves its destination path, handles duplicates, then
     * copies or moves it.
     *
     * @param sourceFile      the file to process
     * @param destinationRoot root destination directory
     * @param date            the detected date, or null to place in "unknown"
     * @param move            true to move, false to copy
     * @param noMonth         true to skip month subdirectory
     * @param dryRun          true to skip actual disk writes
     * @param verbose         true to print detailed output
     * @return the final destination path (may not exist if dryRun)
     */
    public Path process(
            Path sourceFile,
            Path destinationRoot,
            LocalDateTime date,
            boolean move,
            boolean noMonth,
            boolean dryRun,
            boolean verbose
    ) throws IOException {
        Path targetDir = resolveTargetDir(destinationRoot, date, noMonth);
        Path targetFile = resolveTargetFile(targetDir, sourceFile.getFileName().toString(), dryRun);

        if (verbose) {
            String action = dryRun ? "[DRY-RUN] " : "";
            System.out.printf("%s%s %s -> %s%n",
                    action, move ? "MOVE" : "COPY", sourceFile, targetFile);
        }

        if (!dryRun) {
            Files.createDirectories(targetDir);
            if (move) {
                Files.move(sourceFile, targetFile);
            } else {
                Files.copy(sourceFile, targetFile, StandardCopyOption.COPY_ATTRIBUTES);
            }
        }

        return targetFile;
    }

    private Path resolveTargetDir(Path root, LocalDateTime date, boolean noMonth) {
        if (date == null) {
            return root.resolve("unknown");
        }
        String year = String.valueOf(date.getYear());
        if (noMonth) {
            return root.resolve(year);
        }
        int m = date.getMonthValue();
        String monthDir = String.format("%02d_%s", m, MONTH_NAMES[m - 1]);
        return root.resolve(year).resolve(monthDir);
    }

    /**
     * Finds an available filename in targetDir, adding _001, _002 suffixes on collision.
     */
    private Path resolveTargetFile(Path targetDir, String filename, boolean dryRun) {
        Path candidate = targetDir.resolve(filename);

        // In dry-run mode we cannot check the real filesystem; just return as-is.
        if (dryRun) return candidate;

        if (!Files.exists(candidate)) return candidate;

        int dot = filename.lastIndexOf('.');
        String base = dot >= 0 ? filename.substring(0, dot) : filename;
        String ext  = dot >= 0 ? filename.substring(dot) : "";

        for (int i = 1; i <= 999; i++) {
            String suffixed = String.format("%s_%03d%s", base, i, ext);
            candidate = targetDir.resolve(suffixed);
            if (!Files.exists(candidate)) return candidate;
        }

        // Extremely unlikely: fall back to a timestamp-based name
        return targetDir.resolve(base + "_" + System.currentTimeMillis() + ext);
    }
}
