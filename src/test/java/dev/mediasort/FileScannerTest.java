// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FileScannerTest {

    private final FileScanner scanner = new FileScanner();

    @TempDir Path sourceDir;

    @Test
    void emptyDirectoryReturnsEmptyList() throws IOException {
        assertThat(scanner.scan(sourceDir)).isEmpty();
    }

    @Test
    void directoryWithNoMediaFilesReturnsEmptyList() throws IOException {
        Files.write(sourceDir.resolve("notes.txt"), new byte[0]);
        Files.write(sourceDir.resolve("data.xml"), new byte[0]);
        Files.write(sourceDir.resolve("archive.zip"), new byte[0]);

        assertThat(scanner.scan(sourceDir)).isEmpty();
    }

    @Test
    void findsImageAndVideoFilesAtRootLevel() throws IOException {
        Files.write(sourceDir.resolve("photo.jpg"), new byte[0]);
        Files.write(sourceDir.resolve("video.mp4"), new byte[0]);
        Files.write(sourceDir.resolve("ignore.pdf"), new byte[0]);

        List<Path> found = scanner.scan(sourceDir);

        assertThat(found).hasSize(2);
        assertThat(found).extracting(p -> p.getFileName().toString())
                .containsExactlyInAnyOrder("photo.jpg", "video.mp4");
    }

    @Test
    void scansSubdirectoriesRecursively() throws IOException {
        Path level1 = Files.createDirectory(sourceDir.resolve("2023"));
        Path level2 = Files.createDirectory(level1.resolve("08_august"));
        Files.write(level2.resolve("nested_deep.jpg"), new byte[0]);
        Files.write(level1.resolve("nested_one.jpg"), new byte[0]);
        Files.write(sourceDir.resolve("root.jpg"), new byte[0]);

        assertThat(scanner.scan(sourceDir)).hasSize(3);
    }

    @Test
    void allSupportedImageExtensionsAreDetected() throws IOException {
        String[] images = {
                "a.jpg", "b.jpeg", "c.png", "d.gif", "e.bmp",
                "f.webp", "g.heic", "h.heif", "i.tiff", "j.tif",
                "k.raw", "l.cr2", "m.cr3", "n.nef", "o.arw",
                "p.dng", "q.orf", "r.rw2"
        };
        for (String name : images) Files.write(sourceDir.resolve(name), new byte[0]);

        assertThat(scanner.scan(sourceDir)).hasSize(images.length);
    }

    @Test
    void allSupportedVideoExtensionsAreDetected() throws IOException {
        String[] videos = {"a.mp4", "b.mov", "c.avi", "d.mkv", "e.wmv",
                           "f.m4v", "g.3gp", "h.mts", "i.m2ts"};
        for (String name : videos) Files.write(sourceDir.resolve(name), new byte[0]);

        assertThat(scanner.scan(sourceDir)).hasSize(videos.length);
    }

    @Test
    void jsonSidecarFilesAreNotScanned() throws IOException {
        Files.write(sourceDir.resolve("photo.jpg"), new byte[0]);
        Files.write(sourceDir.resolve("photo.jpg.json"), new byte[0]);

        List<Path> found = scanner.scan(sourceDir);

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getFileName().toString()).isEqualTo("photo.jpg");
    }

    @Test
    void subdirectoriesAreNotReturnedAsFiles() throws IOException {
        Files.createDirectory(sourceDir.resolve("subdir"));
        Files.write(sourceDir.resolve("subdir").resolve("photo.jpg"), new byte[0]);

        List<Path> found = scanner.scan(sourceDir);

        assertThat(found).hasSize(1);
        assertThat(found).allMatch(Files::isRegularFile);
    }
}
