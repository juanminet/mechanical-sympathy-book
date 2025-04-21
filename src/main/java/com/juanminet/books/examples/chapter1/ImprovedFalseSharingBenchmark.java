package com.juanminet.books.examples.chapter1;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class ImprovedFalseSharingBenchmark {

    interface CounterInterface {
        void increment(int index);
        long getCount(int index);
    }

    // Regular array with volatile longs
    static class VolatileCounterArray implements CounterInterface {
        private volatile long[] counters;

        public VolatileCounterArray(int size) {
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

    // Padded version with same volatility semantics
    static class PaddedCounterArray implements CounterInterface {
        private static final int PADDING_SIZE = 7;

        static class PaddedCounter {
            public volatile long value;
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

    // A version that demonstrates cross-thread contention
    static class ContendedCounterBenchmark {
        private static final int READER_THREADS = 2;

        public static void runTest(CounterInterface counter, int threads, long iterations) throws Exception {
            Thread[] writerThreads = new Thread[threads];
            Thread[] readerThreads = new Thread[READER_THREADS];
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threads + READER_THREADS);

            // Create writer threads
            for (int i = 0; i < threads; i++) {
                final int index = i;
                writerThreads[i] = new Thread(() -> {
                    try {
                        startLatch.await();
                        for (long j = 0; j < iterations; j++) {
                            counter.increment(index);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            // Create reader threads that read from all counters
            for (int i = 0; i < READER_THREADS; i++) {
                readerThreads[i] = new Thread(() -> {
                    try {
                        startLatch.await();
                        long sum = 0;
                        for (long j = 0; j < iterations; j++) {
                            // Read from all counters round-robin to create contention
                            for (int k = 0; k < threads; k++) {
                                sum += counter.getCount(k);
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            // Start all threads
            for (Thread t : writerThreads) t.start();
            for (Thread t : readerThreads) t.start();

            // Begin the test
            long startTime = System.nanoTime();
            startLatch.countDown();

            // Wait for completion
            endLatch.await(5, TimeUnit.MINUTES);
            long endTime = System.nanoTime();

            long durationMs = (endTime - startTime) / 1_000_000;
            System.out.printf("Time: %d ms\n", durationMs);
        }
    }

    public static void main(String[] args) throws Exception {
        int threadCount = Runtime.getRuntime().availableProcessors();
        System.out.println("Running benchmark with " + threadCount + " threads");

        final long iterationsPerThread = 10_000_000;

        // Warm up
        System.out.println("Warming up...");
        for(int i = 0; i < 100; i++) {
            runBenchmark(new VolatileCounterArray(threadCount), threadCount, 100_000);
            runBenchmark(new PaddedCounterArray(threadCount), threadCount, 100_000);
        }
        // Part 1: Simple increments (no cross-thread contention)
        System.out.println("\nRunning simple increment benchmark...");

        long standardTime = runBenchmark(new VolatileCounterArray(threadCount),
                threadCount, iterationsPerThread);

        long paddedTime = runBenchmark(new PaddedCounterArray(threadCount),
                threadCount, iterationsPerThread);

        System.out.printf("\nSimple increment test - standard:padded ratio: %.2f\n",
                (double) standardTime / paddedTime);

        // Part 2: With cross-thread contention
        System.out.println("\nRunning contended benchmark (with readers)...");

        System.out.print("Standard array with contention: ");
        ContendedCounterBenchmark.runTest(new VolatileCounterArray(threadCount),
                threadCount, iterationsPerThread / 100);

        System.out.print("Padded array with contention: ");
        ContendedCounterBenchmark.runTest(new PaddedCounterArray(threadCount),
                threadCount, iterationsPerThread / 100);
    }

    private static long runBenchmark(CounterInterface counter, int threadCount, long iterations)
            throws Exception {
        Thread[] threads = new Thread[threadCount];
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    startLatch.await();
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

        long startTime = System.nanoTime();
        startLatch.countDown();
        endLatch.await();
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;

        String type = (counter instanceof PaddedCounterArray) ?
                "Padded array" : "Standard array";

        System.out.printf("%s: %d ms\n", type, durationMs);

        return durationMs;
    }
}