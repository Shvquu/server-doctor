package com.serverdoctor.core.env;

/** Supplies JVM/runtime facts. */
public interface RuntimeProbe {
    RuntimeInfo sample();
}
