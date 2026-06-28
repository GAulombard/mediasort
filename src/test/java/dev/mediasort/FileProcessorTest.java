// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FileProcessorTest {

    private final FileProcessor processor = new FileProcessor();

    @TempDir Path sourceDir;
    @TempDir Path destDir;

    private static final LocalDateTime AUG_2023 = LocalDateTime.of(2023, 8, 15, 10, 30);

    // ---- Directory structure ----

    @Test
    void copyCreatesFileUnderYearAndMonthDirectories() throws IOException {
        Path source = write("photo.jpg");

        Path result = processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(result).exists();
        assertThat(result.getParent().getFileName().toString()).isEqualTo("08_august");
        assertThat(result.getParent().getParent().getFileName().toString()).isEqualTo("2023");
        assertThat(result.getFileName().toString()).isEqualTo("photo.jpg");
    }

    @Test
    void noMonthOptionPlacesFileDirectlyUnderYear() throws IOException {
        Path source = write("photo.jpg");

        Path result = processor.process(source, destDir, AUG_2023, false, true, false, false);

        assertThat(result.getParent().getFileName().toString()).isEqualTo("2023");
    }

    @Test
    void nullDatePlacesFileInUnknownDirectory() throws IOException {
        Path source = write("photo.jpg");

        Path result = processor.process(source, destDir, null, false, false, false, false);

        assertThat(result.getParent().getFileName().toString()).isEqualTo("unknown");
        assertThat(result).exists();
    }

    @Test
    void correctMonthFolderNameForEachMonth() throws IOException {
        String[] expected = {
                "01_january", "02_february", "03_march", "04_april",
                "05_may", "06_june", "07_july", "08_august",
                "09_september", "10_october", "11_november", "12_december"
        };
        for (int m = 1; m <= 12; m++) {
            Path source = write("photo" + m + ".jpg");
            LocalDateTime date = LocalDateTime.of(2024, m, 1, 0, 0);
            Path result = processor.process(source, destDir, date, false, false, false, false);
            assertThat(result.getParent().getFileName().toString())
                    .as("Month %d", m)
                    .isEqualTo(expected[m - 1]);
        }
    }

    // ---- Copy vs Move ----

    @Test
    void copyKeepsSourceIntact() throws IOException {
        Path source = write("photo.jpg");

        processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(source).exists();
    }

    @Test
    void moveDeletesSource() throws IOException {
        Path source = write("photo.jpg");

        processor.process(source, destDir, AUG_2023, true, false, false, false);

        assertThat(source).doesNotExist();
    }

    @Test
    void movedFileExistsAtDestination() throws IOException {
        Path source = write("video.mp4");

        Path result = processor.process(source, destDir, AUG_2023, true, false, false, false);

        assertThat(result).exists();
    }

    // ---- Dry run ----

    @Test
    void dryRunDoesNotCreateAnyFile() throws IOException {
        Path source = write("photo.jpg");

        Path result = processor.process(source, destDir, AUG_2023, false, false, true, false);

        assertThat(result).doesNotExist();
        assertThat(source).exists();
    }

    @Test
    void dryRunReturnsExpectedDestinationPath() throws IOException {
        Path source = write("photo.jpg");

        Path result = processor.process(source, destDir, AUG_2023, false, false, true, false);

        assertThat(result.toString()).contains("2023");
        assertThat(result.toString()).contains("08_august");
    }

    // ---- Duplicate handling ----

    @Test
    void firstDuplicateGetsSuffix001() throws IOException {
        Path source = write("photo.jpg");

        processor.process(source, destDir, AUG_2023, false, false, false, false);
        Path second = processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(second.getFileName().toString()).isEqualTo("photo_001.jpg");
        assertThat(second).exists();
    }

    @Test
    void secondDuplicateGetsSuffix002() throws IOException {
        Path source = write("photo.jpg");

        processor.process(source, destDir, AUG_2023, false, false, false, false);
        processor.process(source, destDir, AUG_2023, false, false, false, false);
        Path third = processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(third.getFileName().toString()).isEqualTo("photo_002.jpg");
        assertThat(third).exists();
    }

    @Test
    void duplicateHandlingPreservesExtension() throws IOException {
        Path source = write("clip.mp4");

        processor.process(source, destDir, AUG_2023, false, false, false, false);
        Path second = processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(second.getFileName().toString()).isEqualTo("clip_001.mp4");
    }

    @Test
    void fileWithoutExtensionIsHandled() throws IOException {
        Path source = Files.write(sourceDir.resolve("noext"), new byte[]{1, 2, 3});

        Path result = processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(result).exists();
        assertThat(result.getFileName().toString()).isEqualTo("noext");
    }

    @Test
    void duplicateWithoutExtensionGetsSuffix() throws IOException {
        Path source = Files.write(sourceDir.resolve("noext"), new byte[]{1, 2, 3});

        processor.process(source, destDir, AUG_2023, false, false, false, false);
        Path second = processor.process(source, destDir, AUG_2023, false, false, false, false);

        assertThat(second.getFileName().toString()).isEqualTo("noext_001");
    }

    // ---- Helper ----

    private Path write(String filename) throws IOException {
        return Files.write(sourceDir.resolve(filename), new byte[]{1, 2, 3});
    }
}
