// SPDX-License-Identifier: MIT
// Copyright (c) 2026 Hodor
package dev.mediasort;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe mutable accumulator for processing statistics.
 * Call {@link #snapshot(long)} at the end to get an immutable {@link Result}.
 */
public class Stats {

    private final AtomicInteger total   = new AtomicInteger();
    private final AtomicInteger unknown = new AtomicInteger();
    private final AtomicInteger errors  = new AtomicInteger();
    private final ConcurrentHashMap<Integer, AtomicInteger> byYear = new ConcurrentHashMap<>();

    public void incrementTotal()   { total.incrementAndGet(); }
    public void incrementUnknown() { unknown.incrementAndGet(); }
    public void incrementErrors()  { errors.incrementAndGet(); }

    public void incrementYear(int year) {
        byYear.computeIfAbsent(year, y -> new AtomicInteger()).incrementAndGet();
    }

    /** Returns an immutable snapshot of the current stats. */
    public Result snapshot(long elapsedMs) {
        Map<Integer, Integer> yearMap = new TreeMap<>();
        byYear.forEach((y, c) -> yearMap.put(y, c.get()));
        return new Result(total.get(), yearMap, unknown.get(), errors.get(), elapsedMs);
    }

    /**
     * Immutable stats result printed at the end of execution.
     */
    public record Result(
            int total,
            Map<Integer, Integer> byYear,
            int unknown,
            int errors,
            long elapsedMs
    ) {
        public void print() {
            System.out.println("\n=== STATISTICS ===");
            System.out.printf("Files processed  : %d%n", total);

            var years = byYear.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList();
            for (int i = 0; i < years.size(); i++) {
                var e = years.get(i);
                boolean last = (i == years.size() - 1) && unknown == 0;
                System.out.printf("  %s Year %d: %d%n", last ? "└─" : "├─", e.getKey(), e.getValue());
            }
            if (unknown > 0) {
                System.out.printf("  └─ Unknown    : %d%n", unknown);
            }

            System.out.printf("Errors           : %d%n", errors);
            System.out.printf("Elapsed          : %.1fs%n", elapsedMs / 1000.0);
            System.out.println("==================");
        }
    }
}
