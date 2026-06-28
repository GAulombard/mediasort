// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.nio.file.Path;
import java.util.Set;

/**
 * Sealed interface representing a supported media file type.
 */
public sealed interface MediaType permits MediaType.Image, MediaType.Video {

    record Image(String extension) implements MediaType {}
    record Video(String extension) implements MediaType {}

    Set<String> IMAGE_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "heif", "tiff", "tif",
            "raw", "cr2", "cr3", "nef", "arw", "dng", "orf", "rw2"
    );

    Set<String> VIDEO_EXTENSIONS = Set.of(
            "mp4", "mov", "avi", "mkv", "wmv", "m4v", "3gp", "mts", "m2ts"
    );

    /** Returns the MediaType for the given path, or null if unsupported. */
    static MediaType from(Path file) {
        return from(file.getFileName().toString());
    }

    /** Returns the MediaType for the given filename, or null if unsupported. */
    static MediaType from(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return null;
        String ext = filename.substring(dot + 1).toLowerCase();
        if (IMAGE_EXTENSIONS.contains(ext)) return new Image(ext);
        if (VIDEO_EXTENSIONS.contains(ext)) return new Video(ext);
        return null;
    }
}
