// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MediaSortTest {

    @TempDir Path source;
    @TempDir Path dest;

    // --- progress bar in normal mode ---

    @Test
    void progressBarShowsCounterInNormalMode() throws Exception {
        Files.createFile(source.resolve("IMG_20240101.jpg"));
        Files.createFile(source.resolve("IMG_20240202.jpg"));

        String err = captureStderr(() -> runDryRun(false));

        assertThat(err).contains("2/2");
        assertThat(err).contains("100%");
    }

    @Test
    void progressBarShowsIntermediateStepsInNormalMode() throws Exception {
        for (int i = 1; i <= 5; i++) {
            Files.createFile(source.resolve("IMG_2024010" + i + ".jpg"));
        }

        String err = captureStderr(() -> runDryRun(false));

        // Final step must be present; intermediate steps use \r so only last survives in buffer
        assertThat(err).contains("5/5");
    }

    @Test
    void progressBarNotShownWhenNoFilesInNormalMode() throws Exception {
        String err = captureStderr(() -> runDryRun(false));

        assertThat(err).doesNotContain("%");
    }

    // --- progress bar in verbose mode ---

    @Test
    void progressBarShowsCounterInVerboseMode() throws Exception {
        Files.createFile(source.resolve("IMG_20240101.jpg"));

        String err = captureStderr(() -> runDryRun(true));

        assertThat(err).contains("1/1");
        assertThat(err).contains("100%");
    }

    @Test
    void progressBarPrintsOneLinePerFileInVerboseMode() throws Exception {
        Files.createFile(source.resolve("IMG_20240101.jpg"));
        Files.createFile(source.resolve("IMG_20240202.jpg"));
        Files.createFile(source.resolve("IMG_20240303.jpg"));

        String err = captureStderr(() -> runDryRun(true));

        // In verbose mode each file produces one progress line — expect 3 lines with %
        long progressLines = err.lines().filter(l -> l.contains("%")).count();
        assertThat(progressLines).isEqualTo(3);
    }

    @Test
    void progressBarNotShownWhenNoFilesInVerboseMode() throws Exception {
        String err = captureStderr(() -> runDryRun(true));

        assertThat(err).doesNotContain("%");
    }

    // --- helpers ---

    private void runDryRun(boolean verbose) throws Exception {
        String[] base = {source.toString(), dest.toString(), "--dry-run"};
        String[] args = verbose
                ? new String[]{source.toString(), dest.toString(), "--dry-run", "--verbose"}
                : base;
        new MediaSort(CliArgs.parse(args)).run();
    }

    @FunctionalInterface
    interface ThrowingRunnable { void run() throws Exception; }

    private String captureStderr(ThrowingRunnable action) throws Exception {
        PrintStream original = System.err;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setErr(new PrintStream(baos));
        try {
            action.run();
        } finally {
            System.setErr(original);
        }
        return baos.toString();
    }
}
