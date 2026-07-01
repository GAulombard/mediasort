// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class DateExtractorTest {

    private final DateExtractor extractor = new DateExtractor();

    @TempDir Path tempDir;

    // ---- Filename patterns ----

    @ParameterizedTest
    @CsvSource({
            "IMG_20230815_photo.jpg,      2023, 8,  15",
            "IMG_20190101_test.jpeg,      2019, 1,  1",
            "VID-20241205_video.mp4,      2024, 12, 5",
            "Screenshot_2024-06-01.png,   2024, 6,  1",
            "Screenshot_2022-11-30_ui.jpg,2022, 11, 30",
            "IMG-20161113-WA0003.jpg,     2016, 11, 13",
            "WhatsApp Image 2024-02-27 à 12.00.07_15cf424a.jpg, 2024, 2, 27",
    })
    void filenamePatternExtractsCorrectDate(String filename, int year, int month, int day) throws IOException {
        Path file = Files.write(tempDir.resolve(filename), new byte[0]);

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.FILENAME);
        assertThat(result.date()).isPresent();
        LocalDateTime date = result.date().get();
        assertThat(date.getYear()).isEqualTo(year);
        assertThat(date.getMonthValue()).isEqualTo(month);
        assertThat(date.getDayOfMonth()).isEqualTo(day);
    }

    @Test
    void dateTimePatternExtractsFullDateTime() throws IOException {
        Path file = Files.write(tempDir.resolve("20190320_143015_timelapse.mov"), new byte[0]);

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.FILENAME);
        LocalDateTime date = result.date().get();
        assertThat(date.getYear()).isEqualTo(2019);
        assertThat(date.getMonthValue()).isEqualTo(3);
        assertThat(date.getDayOfMonth()).isEqualTo(20);
        assertThat(date.getHour()).isEqualTo(14);
        assertThat(date.getMinute()).isEqualTo(30);
        assertThat(date.getSecond()).isEqualTo(15);
    }

    @Test
    void filenameWithNoPatternReturnsUnknown() throws IOException {
        Path file = Files.write(tempDir.resolve("random_nodate.jpg"), new byte[0]);

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.UNKNOWN);
        assertThat(result.date()).isEmpty();
    }

    @Test
    void outOfRangeMonthInFilenameIsIgnored() throws IOException {
        // Month 13 is invalid → no strategy matches → UNKNOWN
        Path file = Files.write(tempDir.resolve("IMG_20231399_bad.jpg"), new byte[0]);

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.UNKNOWN);
    }

    // ---- JSON sidecar ----

    @Test
    void jsonSidecarWithPhotoTakenTimeIsUsed() throws IOException {
        Path file = Files.write(tempDir.resolve("photo.jpg"), new byte[0]);
        // 2021-06-15 12:00:00 UTC — safely mid-day to avoid timezone boundary issues
        long epoch = LocalDateTime.of(2021, 6, 15, 12, 0).toEpochSecond(ZoneOffset.UTC);
        writeSidecar(file, "{\"photoTakenTime\":{\"timestamp\":\"" + epoch + "\"}}");

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.JSON);
        assertThat(result.date().get().getYear()).isEqualTo(2021);
    }

    @Test
    void jsonSidecarWithCreationTimeFallback() throws IOException {
        Path file = Files.write(tempDir.resolve("photo.jpg"), new byte[0]);
        long epoch = LocalDateTime.of(2020, 6, 15, 12, 0).toEpochSecond(ZoneOffset.UTC);
        writeSidecar(file, "{\"creationTime\":{\"timestamp\":\"" + epoch + "\"}}");

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.JSON);
        assertThat(result.date().get().getYear()).isEqualTo(2020);
    }

    @Test
    void jsonSidecarTakesPriorityOverFilenamePattern() throws IOException {
        // Filename says 2023, JSON says 2018 → JSON wins
        Path file = Files.write(tempDir.resolve("IMG_20230815_photo.jpg"), new byte[0]);
        long epoch = LocalDateTime.of(2018, 3, 10, 12, 0).toEpochSecond(ZoneOffset.UTC);
        writeSidecar(file, "{\"photoTakenTime\":{\"timestamp\":\"" + epoch + "\"}}");

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.JSON);
        assertThat(result.date().get().getYear()).isEqualTo(2018);
    }

    @Test
    void missingJsonSidecarSkipsJsonStrategy() throws IOException {
        Path file = Files.write(tempDir.resolve("IMG_20230815.jpg"), new byte[0]);
        // No .json file alongside → must not use JSON strategy

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isNotEqualTo(DateExtractor.DateSource.JSON);
    }

    @Test
    void malformedJsonSidecarIsIgnored() throws IOException {
        Path file = Files.write(tempDir.resolve("photo.jpg"), new byte[0]);
        writeSidecar(file, "{ not valid json {{{{");

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isNotEqualTo(DateExtractor.DateSource.JSON);
    }

    @Test
    void jsonSidecarWithoutTimestampFieldIsIgnored() throws IOException {
        Path file = Files.write(tempDir.resolve("photo.jpg"), new byte[0]);
        writeSidecar(file, "{\"description\":\"holiday\",\"tags\":[\"beach\"]}");

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isNotEqualTo(DateExtractor.DateSource.JSON);
    }

    // ---- EXIF ----

    @Test
    void exifFailsGracefullyOnNonJpegContent() throws IOException {
        // Plain-text bytes in a .jpg file → EXIF extraction fails, falls to filename pattern
        Path file = Files.write(tempDir.resolve("IMG_20231001_test.jpg"), "not a jpeg".getBytes());

        DateExtractor.DateResult result = extractor.extract(file);

        // Falls through to FILENAME pattern
        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.FILENAME);
        assertThat(result.date().get().getYear()).isEqualTo(2023);
    }

    @Test
    void exifFailsGracefullyOnEmptyFile() throws IOException {
        Path file = Files.write(tempDir.resolve("IMG_20221205.jpg"), new byte[0]);

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isIn(DateExtractor.DateSource.FILENAME, DateExtractor.DateSource.UNKNOWN);
    }

    // ---- Unknown (no date detectable) ----

    @Test
    void fileWithNoDatableInfoReturnsUnknown() throws IOException {
        Path file = Files.write(tempDir.resolve("nodatefile.jpg"), new byte[0]);

        DateExtractor.DateResult result = extractor.extract(file);

        assertThat(result.source()).isEqualTo(DateExtractor.DateSource.UNKNOWN);
        assertThat(result.date()).isEmpty();
    }

    // ---- Helper ----

    private void writeSidecar(Path mediaFile, String json) throws IOException {
        Files.writeString(mediaFile.resolveSibling(mediaFile.getFileName() + ".json"), json);
    }
}
