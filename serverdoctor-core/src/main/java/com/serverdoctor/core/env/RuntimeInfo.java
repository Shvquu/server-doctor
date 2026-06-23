package com.serverdoctor.core.env;

import java.util.List;

/** JVM facts relevant to server health. heap/ram in bytes; jvmArgs are the JVM input arguments. */
public record RuntimeInfo(int javaMajor, long maxHeapBytes, long totalRamBytes, List<String> jvmArgs) {
    public RuntimeInfo {
        jvmArgs = jvmArgs == null ? List.of() : List.copyOf(jvmArgs);
    }
}
