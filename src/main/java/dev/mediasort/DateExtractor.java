// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the best available date from a media file using 4 strategies in priority order.
 */
public class DateExtractor {

    public enum DateSource { JSON, EXIF, FILENAME, MODIFIED }

    /**
     * Result of a date extraction attempt.
     *
     * @param date       the extracted date, if any
     * @param source     which strategy produced the result
     * @param isReliable false when the source is the modification date fallback
     */
    public record DateResult(
            Optional<LocalDateTime> date,
            DateSource source,
            boolean isReliable
    ) {}

    // Patterns ordered by specificity
    private static final Pattern P_IMG      = Pattern.compile("IMG_(\\d{4})(\\d{2})(\\d{2})");
    private static final Pattern P_VID      = Pattern.compile("VID-(\\d{4})(\\d{2})(\\d{2})");
    private static final Pattern P_DATETIME = Pattern.compile("(\\d{4})(\\d{2})(\\d{2})_(\\d{2})(\\d{2})(\\d{2})");
    private static final Pattern P_SCREEN   = Pattern.compile("Screenshot_(\\d{4})-(\\d{2})-(\\d{2})");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** Tries all 4 strategies in priority order and returns the first successful result. */
    public DateResult extract(Path file) {
        var json = fromJson(file);
        if (json.isPresent()) return new DateResult(json, DateSource.JSON, true);

        var exif = fromExif(file);
        if (exif.isPresent()) return new DateResult(exif, DateSource.EXIF, true);

        var name = fromFilename(file.getFileName().toString());
        if (name.isPresent()) return new DateResult(name, DateSource.FILENAME, true);

        var mod = fromModified(file);
        return new DateResult(mod, DateSource.MODIFIED, false);
    }

    // --- Strategy 1: Google Takeout JSON sidecar ---

    private Optional<LocalDateTime> fromJson(Path mediaFile) {
        String filename = mediaFile.getFileName().toString();
        Path jsonFile = mediaFile.resolveSibling(filename + ".json");

        if (!Files.exists(jsonFile)) return Optional.empty();

        try {
            JsonNode root = MAPPER.readTree(jsonFile.toFile());
            JsonNode ts = root.path("photoTakenTime").path("timestamp");
            if (ts.isMissingNode()) ts = root.path("creationTime").path("timestamp");
            if (ts.isMissingNode()) return Optional.empty();

            long epoch = ts.asLong();
            return Optional.of(LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    // --- Strategy 2: EXIF metadata ---

    private Optional<LocalDateTime> fromExif(Path file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.toFile());
            var dir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (dir == null) return Optional.empty();

            var date = dir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (date == null) date = dir.getDate(ExifSubIFDDirectory.TAG_DATETIME_DIGITIZED);
            if (date == null) return Optional.empty();

            return Optional.of(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // --- Strategy 3: filename patterns ---

    private Optional<LocalDateTime> fromFilename(String filename) {
        // YYYYMMDD_HHmmss (most specific, check first)
        Matcher m = P_DATETIME.matcher(filename);
        if (m.find()) return toDateTime(m.group(1), m.group(2), m.group(3), m.group(4), m.group(5), m.group(6));

        m = P_IMG.matcher(filename);
        if (m.find()) return toDate(m.group(1), m.group(2), m.group(3));

        m = P_VID.matcher(filename);
        if (m.find()) return toDate(m.group(1), m.group(2), m.group(3));

        m = P_SCREEN.matcher(filename);
        if (m.find()) return toDate(m.group(1), m.group(2), m.group(3));

        return Optional.empty();
    }

    private Optional<LocalDateTime> toDate(String year, String month, String day) {
        return toDateTime(year, month, day, "0", "0", "0");
    }

    private Optional<LocalDateTime> toDateTime(String y, String mo, String d, String h, String mi, String s) {
        try {
            int year  = Integer.parseInt(y);
            int month = Integer.parseInt(mo);
            int day   = Integer.parseInt(d);
            int hour  = Integer.parseInt(h);
            int min   = Integer.parseInt(mi);
            int sec   = Integer.parseInt(s);

            if (year < 1900 || year > 2100 || month < 1 || month > 12 || day < 1 || day > 31) {
                return Optional.empty();
            }
            return Optional.of(LocalDateTime.of(year, month, day, hour, min, sec));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    // --- Strategy 4: file modification date (fallback) ---

    private Optional<LocalDateTime> fromModified(Path file) {
        try {
            var time = Files.getLastModifiedTime(file);
            return Optional.of(LocalDateTime.ofInstant(time.toInstant(), ZoneId.systemDefault()));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
