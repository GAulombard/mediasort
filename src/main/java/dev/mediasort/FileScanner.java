// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

/**
 * Recursively scans a source directory and returns all supported media files.
 */
public class FileScanner {

    /**
     * Walks {@code source} recursively and returns all files whose extension
     * is recognised by {@link MediaType}.
     */
    public List<Path> scan(Path source) throws IOException {
        try (Stream<Path> walk = Files.walk(source)) {
            return walk
                    .filter(Files::isRegularFile)
                    .filter(p -> MediaType.from(p) != null)
                    .toList();
        }
    }
}
