// Example 1.3: Simple JMH benchmark
package com.juanminet.books.examples.chapter1;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class StringConcatenationBenchmark {

    @Param({"10", "100", "1000"})
    private int iterations;

    @Benchmark
    public String concatenateWithPlus() {
        String result = "";
        for (int i = 0; i < iterations; i++) {
            result += "a";
        }
        return result;
    }

    @Benchmark
    public String concatenateWithStringBuilder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < iterations; i++) {
            sb.append("a");
        }
        return sb.toString();
    }

    public static void main(String[] args) throws Exception {
        Options opt = new OptionsBuilder()
                .include(StringConcatenationBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
