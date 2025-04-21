package com.juanminet.books.examples.chapter1;

import java.util.concurrent.CyclicBarrier;

public class FalseSharingBenchmark {

    private static final int CACHE_LINE_SIZE = 64; // 64 bytes is common on modern CPUs

    // Interface for both counter implementations
    interface Counter {
        void increment(int index);
        long get(int index);
    }

    // Implementation with false sharing
    static final class SharedCounter implements Counter {
        // Counters are adjacent in memory - will experience false sharing
        private final long[] counters;

        public SharedCounter(int numCounters) {
            counters = new long[numCounters];
        }

        @Override
        public void increment(int index) {
            counters[index]++;
        }

        @Override
        public long get(int index) {
            return counters[index];
        }
    }

    // Implementation that avoids false sharing
    static final class PaddedCounter implements Counter {
        // Each counter is padded to avoid false sharing
        private static final int PADDING_ELEMENTS = CACHE_LINE_SIZE / 8;
        private final long[][] counters;

        public PaddedCounter(int numCounters) {
            counters = new long[numCounters][PADDING_ELEMENTS];
        }

        @Override
        public void increment(int index) {
            counters[index][0]++; // Only use first element of each padded array
        }

        @Override
        public long get(int index) {
            return counters[index][0];
        }
    }

    public static void main(String[] args) throws Exception {
        int numThreads = Runtime.getRuntime().availableProcessors();
        System.out.println("Running with " + numThreads + " threads on " +
                numThreads + " cores");

        long iterations = 500_000_000; // Large enough to see the effect

        // Warm up
        System.out.println("Warming up (this may take a moment)...");
        for (int i = 0; i < 10; i++) { // More warm-up rounds
            runBenchmark(new SharedCounter(numThreads), numThreads, iterations);
            runBenchmark(new PaddedCounter(numThreads), numThreads, iterations);
        }

        System.out.println("\nRunning benchmarks...");

        int numRuns = 3;
        long[] sharedTimes = new long[numRuns];
        long[] paddedTimes = new long[numRuns];

        for (int i = 0; i < numRuns; i++) {
            System.out.println("\nRun " + (i+1) + ":");
            sharedTimes[i] = runBenchmark(new SharedCounter(numThreads),
                    numThreads, iterations);
            paddedTimes[i] = runBenchmark(new PaddedCounter(numThreads),
                    numThreads, iterations);
        }

        // Calculate average times
        long sharedAvg = average(sharedTimes);
        long paddedAvg = average(paddedTimes);

        System.out.println("\nResults (average of " + numRuns + " runs):");
        System.out.println("Shared counters (with false sharing): " + sharedAvg + " ms");
        System.out.println("Padded counters (without false sharing): " + paddedAvg + " ms");

        double ratio = (double)sharedAvg / paddedAvg;
        System.out.printf("Performance ratio: %.2fx\n", ratio);

        if (ratio > 1.1) {
            System.out.println("The padded version is faster, demonstrating the impact of false sharing!");
        } else if (ratio < 0.9) {
            System.out.println("The shared version is faster. This suggests other factors are dominating.");
        } else {
            System.out.println("No significant difference between implementations.");
        }
    }

    private static long average(long[] values) {
        long sum = 0;
        for (long v : values) sum += v;
        return sum / values.length;
    }

    private static long runBenchmark(Counter counter, int numThreads, long iterations)
            throws Exception {

        final CyclicBarrier startBarrier = new CyclicBarrier(numThreads + 1);
        final CyclicBarrier endBarrier = new CyclicBarrier(numThreads + 1);

        // Create and start threads
        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startBarrier.await(); // Wait for start signal

                    // Each thread updates its own counter
                    for (long j = 0; j < iterations; j++) {
                        counter.increment(index);
                    }

                    endBarrier.await(); // Signal completion
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        // Start timing
        startBarrier.await();
        long startTime = System.nanoTime();

        // Wait for completion
        endBarrier.await();
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;

        String type = (counter instanceof SharedCounter) ?
                "Shared counters" : "Padded counters";
        System.out.println(type + ": " + durationMs + " ms");

        return durationMs;
    }
}