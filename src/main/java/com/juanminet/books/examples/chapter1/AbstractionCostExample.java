package com.juanminet.books.examples.chapter1;

// Example 1.2: The Cost of Abstraction

import java.util.ArrayList;
import java.util.List;

public class AbstractionCostExample {

    private static final int ITERATIONS = 50_000_000;

    // Direct array access - minimal abstraction
    public static long sumWithArray() {
        int[] numbers = new int[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            numbers[i] = i;
        }

        long sum = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum += numbers[i];
        }
        return sum;
    }

    // Collection API - medium abstraction
    public static long sumWithArrayList() {
        List<Integer> numbers = new ArrayList<>(ITERATIONS);
        for (int i = 0; i < ITERATIONS; i++) {
            numbers.add(i);
        }

        long sum = 0;
        for (int i = 0; i < ITERATIONS; i++) {
            sum += numbers.get(i);
        }
        return sum;
    }

    // Stream API - high abstraction
    public static long sumWithStream() {
        return java.util.stream.IntStream.range(0, ITERATIONS)
                .mapToLong(i -> i)
                .sum();
    }

    public static void main(String[] args) {
        // Warm up (important for fair comparison due to JIT compilation)
        for (int i = 0; i < 10; i++) {
            sumWithArray();
            sumWithArrayList();
            sumWithStream();
        }

        // Measure performance
        long start, end;

        start = System.nanoTime();
        long sum1 = sumWithArray();
        end = System.nanoTime();
        System.out.printf("Array sum: %d, Time: %.3f ms%n",
                sum1, (end - start) / 1_000_000.0);

        start = System.nanoTime();
        long sum2 = sumWithArrayList();
        end = System.nanoTime();
        System.out.printf("ArrayList sum: %d, Time: %.3f ms%n",
                sum2, (end - start) / 1_000_000.0);

        start = System.nanoTime();
        long sum3 = sumWithStream();
        end = System.nanoTime();
        System.out.printf("Stream sum: %d, Time: %.3f ms%n",
                sum3, (end - start) / 1_000_000.0);
    }
}
