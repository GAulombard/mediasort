// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class CliArgsTest {

    @TempDir Path source;
    @TempDir Path dest;

    @Test
    void defaultValuesWhenOnlyPositionalArgs() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString()});

        assertThat(args.source()).isEqualTo(source);
        assertThat(args.destination().toString()).isEqualTo(dest.toString());
        assertThat(args.move()).isFalse();
        assertThat(args.noMonth()).isFalse();
        assertThat(args.dryRun()).isFalse();
        assertThat(args.threads()).isEqualTo(4);
        assertThat(args.verbose()).isFalse();
        assertThat(args.rebuild()).isFalse();
        assertThat(args.excludePatterns()).isEmpty();
        assertThat(args.deleteExcluded()).isFalse();
    }

    @Test
    void destinationDefaultsToSourceSubfolder() {
        CliArgs args = CliArgs.parse(new String[]{source.toString()});

        assertThat(args.source()).isEqualTo(source);
        assertThat(args.destination()).isEqualTo(source.resolve("mediasort"));
    }

    @Test
    void singleExcludePatternIsParsed() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(),
                "--exclude-pattern", "screenshot"});

        assertThat(args.excludePatterns()).containsExactly("screenshot");
    }

    @Test
    void multipleExcludePatternsAreParsed() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(),
                "--exclude-pattern", "screenshot",
                "--exclude-pattern", "IMG_BURST"});

        assertThat(args.excludePatterns()).containsExactly("screenshot", "IMG_BURST");
    }

    @Test
    void deleteExcludedFlagIsRecognised() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(),
                "--exclude-pattern", "screenshot", "--delete-excluded"});

        assertThat(args.deleteExcluded()).isTrue();
    }

    @Test
    void moveFlagIsRecognised() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--move"});
        assertThat(args.move()).isTrue();
    }

    @Test
    void noMonthFlagIsRecognised() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--no-month"});
        assertThat(args.noMonth()).isTrue();
    }

    @Test
    void dryRunFlagIsRecognised() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--dry-run"});
        assertThat(args.dryRun()).isTrue();
    }

    @Test
    void verboseFlagIsRecognised() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--verbose"});
        assertThat(args.verbose()).isTrue();
    }

    @Test
    void rebuildFlagIsRecognised() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--rebuild"});
        assertThat(args.rebuild()).isTrue();
    }

    @Test
    void threadCountIsParsedCorrectly() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--threads=8"});
        assertThat(args.threads()).isEqualTo(8);
    }

    @Test
    void threadsMinimumIsEnforcedAtOne() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--threads=0"});
        assertThat(args.threads()).isEqualTo(1);

        args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--threads=-5"});
        assertThat(args.threads()).isEqualTo(1);
    }

    @Test
    void invalidThreadsValueFallsBackToFour() {
        CliArgs args = CliArgs.parse(new String[]{source.toString(), dest.toString(), "--threads=bad"});
        assertThat(args.threads()).isEqualTo(4);
    }

    @Test
    void allFlagsCanBeCombined() {
        CliArgs args = CliArgs.parse(new String[]{
                source.toString(), dest.toString(),
                "--move", "--no-month", "--dry-run", "--verbose", "--threads=16", "--rebuild"
        });

        assertThat(args.move()).isTrue();
        assertThat(args.noMonth()).isTrue();
        assertThat(args.dryRun()).isTrue();
        assertThat(args.verbose()).isTrue();
        assertThat(args.threads()).isEqualTo(16);
        assertThat(args.rebuild()).isTrue();
    }

    @Test
    void multipleFlagsAfterPositionalArgsAreAllRecognised() {
        CliArgs args = CliArgs.parse(new String[]{
                source.toString(), dest.toString(), "--verbose", "--dry-run"
        });
        assertThat(args.verbose()).isTrue();
        assertThat(args.dryRun()).isTrue();
    }
}
