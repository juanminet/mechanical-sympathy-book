package com.juanminet.books.examples.chapter1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;

/**
 * A benchmark demonstrating the surprising effects when measuring false sharing.
 * This version shows unexpected results due to mixing multiple performance factors.
 */
public class InitialFalseSharingBenchmark {

    // Interface both implementations will use
    interface CounterInterface {
        void increment(int index);
        long getCount(int index);
    }

    // Regular array implementation (should suffer from false sharing)
    static class CounterArray implements CounterInterface {
        private final long[] counters;

        public CounterArray(int size) {
            counters = new long[size];
        }

        @Override
        public void increment(int index) {
            counters[index]++;
        }

        @Override
        public long getCount(int index) {
            return counters[index];
        }
    }

    // Padded implementation (should avoid false sharing)
    static class PaddedCounterArray implements CounterInterface {
        private static final int PADDING_SIZE = 7;

        static class PaddedCounter {
            public volatile long value; // Notice the volatile keyword!
            // Padding to ensure each counter gets its own cache line
            public long p1, p2, p3, p4, p5, p6, p7;
        }

        private final PaddedCounter[] counters;

        public PaddedCounterArray(int size) {
            counters = new PaddedCounter[size];
            for (int i = 0; i < size; i++) {
                counters[i] = new PaddedCounter();
            }
        }

        @Override
        public void increment(int index) {
            counters[index].value++;
        }

        @Override
        public long getCount(int index) {
            return counters[index].value;
        }
    }

    public static void main(String[] args) throws Exception {
        int threadCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Running benchmark with " + threadCount + " threads");

        final long iterationsPerThread = 100_000_000;

        // Warm up
        System.out.println("Warming up...");
        runBenchmark(new CounterArray(threadCount), threadCount, 1_000_000);
        runBenchmark(new PaddedCounterArray(threadCount), threadCount, 1_000_000);

        // Actual benchmark
        System.out.println("\nRunning actual benchmark...");

        long standardTime = runBenchmark(new CounterArray(threadCount),
                threadCount, iterationsPerThread);

        long paddedTime = runBenchmark(new PaddedCounterArray(threadCount),
                threadCount, iterationsPerThread);

        // Calculate and report improvement
        double ratio = (double) standardTime / paddedTime;
        System.out.printf("\nFalse sharing impact: %.2fx performance difference\n", ratio);

        if (ratio > 1.0) {
            System.out.println("The padded version is faster, as expected by theory.");
        } else {
            System.out.println("Surprisingly, the standard version is faster! " +
                    "This contradicts what theory would predict.");
            System.out.println("Can you spot why? Hint: Look for differences beyond just padding.");
        }
    }

    private static long runBenchmark(CounterInterface counter, int threadCount, long iterations)
            throws Exception {
        Thread[] threads = new Thread[threadCount];
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        // Create and start threads
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await(); // Wait for start signal
                    for (long j = 0; j < iterations; j++) {
                        counter.increment(index);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    endLatch.countDown();
                }
            });
            threads[i].start();
        }

        // Start timing
        long startTime = System.nanoTime();
        startLatch.countDown(); // Start all threads

        // Wait for completion
        endLatch.await();
        long endTime = System.nanoTime();

        // Calculate duration and throughput
        long durationMs = (endTime - startTime) / 1_000_000;
        long totalOps = iterations * threadCount;
        double throughput = (totalOps / (durationMs / 1000.0)) / 1_000_000;

        // Report results
        String type = (counter instanceof CounterArray) ?
                "Standard array (with false sharing)" :
                "Padded array (without false sharing)";

        System.out.printf("%s: %d ms\n", type, durationMs);
        System.out.printf("Throughput: %.2f million ops/sec\n", throughput);

        return durationMs;
    }
}