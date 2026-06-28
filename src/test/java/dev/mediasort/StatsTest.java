// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class StatsTest {

    @Test
    void freshStatsSnapshotIsAllZero() {
        Stats stats = new Stats();

        Stats.Result result = stats.snapshot(100);

        assertThat(result.total()).isZero();
        assertThat(result.unknown()).isZero();
        assertThat(result.errors()).isZero();
        assertThat(result.byYear()).isEmpty();
        assertThat(result.elapsedMs()).isEqualTo(100);
    }

    @Test
    void totalCountIsAccumulated() {
        Stats stats = new Stats();

        stats.incrementTotal();
        stats.incrementTotal();
        stats.incrementTotal();

        assertThat(stats.snapshot(0).total()).isEqualTo(3);
    }

    @Test
    void unknownCountIsAccumulated() {
        Stats stats = new Stats();

        stats.incrementUnknown();
        stats.incrementUnknown();

        assertThat(stats.snapshot(0).unknown()).isEqualTo(2);
    }

    @Test
    void errorCountIsAccumulated() {
        Stats stats = new Stats();

        stats.incrementErrors();

        assertThat(stats.snapshot(0).errors()).isEqualTo(1);
    }

    @Test
    void perYearCountsAreAccumulated() {
        Stats stats = new Stats();

        stats.incrementYear(2022);
        stats.incrementYear(2023);
        stats.incrementYear(2023);
        stats.incrementYear(2024);
        stats.incrementYear(2024);
        stats.incrementYear(2024);

        Stats.Result result = stats.snapshot(0);
        assertThat(result.byYear()).containsEntry(2022, 1);
        assertThat(result.byYear()).containsEntry(2023, 2);
        assertThat(result.byYear()).containsEntry(2024, 3);
    }

    @Test
    void byYearIsOrderedAscending() {
        Stats stats = new Stats();
        stats.incrementYear(2025);
        stats.incrementYear(2019);
        stats.incrementYear(2022);

        List<Integer> years = new ArrayList<>(stats.snapshot(0).byYear().keySet());

        assertThat(years).containsExactly(2019, 2022, 2025);
    }

    @Test
    void snapshotsAreIndependentOfFutureIncrements() {
        Stats stats = new Stats();
        stats.incrementTotal();

        Stats.Result before = stats.snapshot(0);
        stats.incrementTotal();
        Stats.Result after = stats.snapshot(0);

        assertThat(before.total()).isEqualTo(1);
        assertThat(after.total()).isEqualTo(2);
    }

    @Test
    void elapsedMsIsPassedThroughToResult() {
        Stats stats = new Stats();

        assertThat(stats.snapshot(1234).elapsedMs()).isEqualTo(1234);
        assertThat(stats.snapshot(9999).elapsedMs()).isEqualTo(9999);
    }

    @Test
    void threadSafeTotalUnderHighConcurrency() throws InterruptedException {
        Stats stats = new Stats();
        int count = 500;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            threads.add(Thread.ofVirtual().start(stats::incrementTotal));
        }
        for (Thread t : threads) t.join();

        assertThat(stats.snapshot(0).total()).isEqualTo(count);
    }

    @Test
    void threadSafeYearCounterUnderHighConcurrency() throws InterruptedException {
        Stats stats = new Stats();
        int count = 300;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int year = 2020 + (i % 3); // spreads across 2020, 2021, 2022
            threads.add(Thread.ofVirtual().start(() -> stats.incrementYear(year)));
        }
        for (Thread t : threads) t.join();

        Stats.Result result = stats.snapshot(0);
        int total = result.byYear().values().stream().mapToInt(Integer::intValue).sum();
        assertThat(total).isEqualTo(count);
        assertThat(result.byYear().get(2020)).isEqualTo(100);
        assertThat(result.byYear().get(2021)).isEqualTo(100);
        assertThat(result.byYear().get(2022)).isEqualTo(100);
    }

    @Test
    void threadSafeMixedOperations() throws InterruptedException {
        Stats stats = new Stats();
        int count = 200;
        List<Thread> threads = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            threads.add(Thread.ofVirtual().start(() -> {
                stats.incrementTotal();
                stats.incrementYear(2024);
                stats.incrementErrors();
            }));
        }
        for (Thread t : threads) t.join();

        Stats.Result result = stats.snapshot(0);
        assertThat(result.total()).isEqualTo(count);
        assertThat(result.byYear().get(2024)).isEqualTo(count);
        assertThat(result.errors()).isEqualTo(count);
    }
}
