// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MediaTypeTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "photo.jpg", "image.jpeg", "picture.png", "anim.gif",
            "file.bmp", "pic.webp", "shot.heic", "img.heif",
            "scan.tiff", "scan.tif", "photo.raw", "photo.cr2",
            "photo.cr3", "photo.nef", "photo.arw", "photo.dng",
            "photo.orf", "photo.rw2"
    })
    void imageExtensionsAreRecognised(String filename) {
        assertThat(MediaType.from(filename)).isInstanceOf(MediaType.Image.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "video.mp4", "clip.mov", "film.avi", "movie.mkv",
            "video.wmv", "clip.m4v", "mobile.3gp", "hd.mts", "hd.m2ts"
    })
    void videoExtensionsAreRecognised(String filename) {
        assertThat(MediaType.from(filename)).isInstanceOf(MediaType.Video.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"document.pdf", "notes.txt", "archive.zip", "data.xml"})
    void unsupportedExtensionsReturnNull(String filename) {
        assertThat(MediaType.from(filename)).isNull();
    }

    @Test
    void fileWithNoDotReturnsNull() {
        assertThat(MediaType.from("noextension")).isNull();
    }

    @Test
    void extensionLookupIsCaseInsensitive() {
        assertThat(MediaType.from("PHOTO.JPG")).isInstanceOf(MediaType.Image.class);
        assertThat(MediaType.from("VIDEO.MOV")).isInstanceOf(MediaType.Video.class);
        assertThat(MediaType.from("clip.MP4")).isInstanceOf(MediaType.Video.class);
    }

    @Test
    void extensionValueIsLowercased() {
        MediaType img = MediaType.from("photo.JPG");
        assertThat(((MediaType.Image) img).extension()).isEqualTo("jpg");

        MediaType vid = MediaType.from("video.MOV");
        assertThat(((MediaType.Video) vid).extension()).isEqualTo("mov");
    }

    @Test
    void fromPathDelegatesToFilename() {
        assertThat(MediaType.from(Path.of("/some/dir/photo.jpg"))).isInstanceOf(MediaType.Image.class);
        assertThat(MediaType.from(Path.of("C:\\Users\\foo\\video.mp4"))).isInstanceOf(MediaType.Video.class);
    }

    @Test
    void dotOnlyFilenameReturnsNull() {
        // edge case: filename "." has no extension after the dot
        assertThat(MediaType.from(".hidden")).isNull();
    }
}
