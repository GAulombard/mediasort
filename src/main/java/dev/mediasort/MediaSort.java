// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

/**
 * Orchestrates the full mediasort pipeline:
 * scan → extract date → process file → collect stats.
 */
public class MediaSort {

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CliArgs args;
    private final FileScanner scanner = new FileScanner();
    private final DateExtractor extractor = new DateExtractor();
    private final FileProcessor processor = new FileProcessor();
    private final Stats stats = new Stats();

    private PrintWriter errorLog;

    public MediaSort(CliArgs args) {
        this.args = args;
    }

    public void run() throws IOException, InterruptedException {
        long start = System.currentTimeMillis();

        if (!args.dryRun()) {
            Files.createDirectories(args.destination());
            initErrorLog();
        }

        System.out.printf("Scanning:    %s%n", args.source());
        System.out.printf("Destination: %s%n", args.destination());
        List<Path> files = scanner.scan(args.source());
        System.out.printf("%d media file(s) found.%n%n", files.size());

        if (args.dryRun()) {
            System.out.println("[DRY-RUN — no files will be written]");
        }

        processWithVirtualThreads(files);

        if (errorLog != null) errorLog.close();

        stats.snapshot(System.currentTimeMillis() - start).print();
    }

    private void processWithVirtualThreads(List<Path> files) throws InterruptedException {
        var latch = new CountDownLatch(files.size());
        // Semaphore caps concurrency to the requested thread count
        var semaphore = new Semaphore(args.threads());

        for (Path file : files) {
            Thread.ofVirtual().start(() -> {
                try {
                    semaphore.acquire();
                    try {
                        processOne(file);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
    }

    private void processOne(Path file) {
        if (isExcluded(file)) {
            handleExcluded(file);
            return;
        }

        stats.incrementTotal();
        try {
            DateExtractor.DateResult result = extractor.extract(file);
            LocalDateTime date = result.date().orElse(null);

            if (date == null) {
                stats.incrementUnknown();
            } else {
                stats.incrementYear(date.getYear());
            }

            processor.process(file, args.destination(), date, args.move(), args.noMonth(), args.dryRun(), args.verbose());

            if (!args.verbose() && result.source() == DateExtractor.DateSource.MODIFIED) {
                System.out.printf("[WARN] Using modification date for: %s%n", file.getFileName());
            }

        } catch (Exception e) {
            stats.incrementErrors();
            String msg = String.format("[ERROR] %s: %s", file, e.getMessage());
            System.err.println(msg);
            logError(file, e);
        }
    }

    private boolean isExcluded(Path file) {
        if (args.excludePatterns().isEmpty()) return false;
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return args.excludePatterns().stream()
                .anyMatch(p -> name.contains(p.toLowerCase(Locale.ROOT)));
    }

    private void handleExcluded(Path file) {
        if (args.dryRun()) {
            System.out.printf("[DRY-RUN] Would exclude: %s%n", file.getFileName());
            return;
        }
        if (args.deleteExcluded()) {
            try {
                Files.delete(file);
                System.out.printf("[DELETED] %s%n", file.getFileName());
            } catch (IOException e) {
                System.err.printf("[ERROR] Could not delete %s: %s%n", file.getFileName(), e.getMessage());
            }
        } else {
            System.out.printf("[EXCLUDED] Skipping: %s%n", file.getFileName());
        }
        stats.incrementExcluded();
    }

    private void initErrorLog() {
        Path logFile = args.destination().resolve("mediasort-errors.log");
        try {
            errorLog = new PrintWriter(Files.newBufferedWriter(logFile));
        } catch (IOException e) {
            System.err.println("[WARN] Could not create error log file: " + e.getMessage());
        }
    }

    private synchronized void logError(Path file, Exception e) {
        if (errorLog == null) return;
        errorLog.printf("[%s] ERROR processing %s: %s%n",
                LocalDateTime.now().format(LOG_FMT), file, e.getMessage());
        errorLog.flush();
    }
}
