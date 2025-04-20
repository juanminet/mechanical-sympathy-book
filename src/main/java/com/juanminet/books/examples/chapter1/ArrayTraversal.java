package com.juanminet.books.examples.chapter1;

// Example 1.1: Array Traversal Patterns

public class ArrayTraversal {

    private static final int WARM_UP_ITERATIONS = 100;
    private static final int MATRIX_SIZE = 5000;

    // Row-major traversal (hardware-friendly for Java arrays)
    public static long sumRowMajor(int[][] array) {
        long sum = 0;
        for (int i = 0; i < array.length; i++) {
            for (int j = 0; j < array[i].length; j++) {
                sum += array[i][j];
                // This access pattern matches how the data is laid out in memory,
                // resulting in efficient cache utilization
            }
        }
        return sum;
    }

    // Column-major traversal (hardware-unfriendly for Java arrays)
    public static long sumColumnMajor(int[][] array) {
        long sum = 0;
        for (int j = 0; j < array[0].length; j++) {
            for (int i = 0; i < array.length; i++) {
                sum += array[i][j];
                // This access pattern jumps across memory regions,
                // causing more cache misses and slower performance
            }
        }
        return sum;
    }

    public static void main(String[] args) {
        // Create a large 2D array
        int size = MATRIX_SIZE;
        int[][] data = new int[size][size];

        //Warm up
        for(int i = 0; i< WARM_UP_ITERATIONS; i++) {
            initializeData(size, data);
            sumRowMajor(data);
            sumColumnMajor(data);
        }

        // Fill with some values
        initializeData(size, data);

        // Measure performance of both approaches
        long start, end;

        start = System.nanoTime();
        long sum1 = sumRowMajor(data);
        end = System.nanoTime();
        System.out.printf("Row-major sum: %d, Time: %.3f ms%n",
                sum1, (end - start) / 1_000_000.0);

        start = System.nanoTime();
        long sum2 = sumColumnMajor(data);
        end = System.nanoTime();
        System.out.printf("Column-major sum: %d, Time: %.3f ms%n",
                sum2, (end - start) / 1_000_000.0);
    }

    private static void initializeData(int size, int[][] data) {
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                data[i][j] = i + j;
            }
        }
    }
}
