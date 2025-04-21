package com.juanminet.books.examples.chapter1;

import java.util.ArrayList;
import java.util.List;

public class MemoryLayoutExample {

    // Object-oriented approach (poor cache utilization)
    static class Point {
        private final double x;
        private final double y;

        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }

        double distanceFromOrigin() {
            return Math.sqrt(x * x + y * y);
        }
    }

    public static double sumDistancesObjects(int size) {
        // Create a list of point objects
        List<Point> points = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            points.add(new Point(i, i));
        }

        // Calculate sum of distances (forces cache misses)
        double sum = 0.0;
        for (Point p : points) {
            sum += p.distanceFromOrigin();
        }
        return sum;
    }

    // Data-oriented approach (better cache utilization)
    public static double sumDistancesArrays(int size) {
        // Store points as separate arrays
        double[] xValues = new double[size];
        double[] yValues = new double[size];

        for (int i = 0; i < size; i++) {
            xValues[i] = i;
            yValues[i] = i;
        }

        // Calculate sum of distances (better cache usage)
        double sum = 0.0;
        for (int i = 0; i < size; i++) {
            double x = xValues[i];
            double y = yValues[i];
            sum += Math.sqrt(x * x + y * y);
        }
        return sum;
    }

    public static void main(String[] args) {
        int size = 10_000_000;

        // Warm up
        for (int i = 0; i < 25; i++) {
            sumDistancesObjects(size / 10);
            sumDistancesArrays(size / 10);
        }

        // Measure performance
        long start, end;

        start = System.nanoTime();
        double sum1 = sumDistancesObjects(size);
        end = System.nanoTime();
        System.out.printf("Object approach: %.2f, Time: %.3f ms%n",
                sum1, (end - start) / 1_000_000.0);

        start = System.nanoTime();
        double sum2 = sumDistancesArrays(size);
        end = System.nanoTime();
        System.out.printf("Array approach: %.2f, Time: %.3f ms%n",
                sum2, (end - start) / 1_000_000.0);
    }
}
