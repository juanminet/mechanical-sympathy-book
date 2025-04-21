package com.juanminet.books.examples.chapter1;

import org.openjdk.jol.info.ClassLayout;
import org.openjdk.jol.vm.VM;

public class JOLObjectSize {
    public static void main(String[] args) {
        System.out.println(VM.current().details());

        SmallObject obj = new SmallObject(42);

        // Print object header and memory layout
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());
    }

    static class SmallObject {
        private final int value; // Just 4 bytes of actual data

        public SmallObject(int value) {
            this.value = value;
        }
    }
}