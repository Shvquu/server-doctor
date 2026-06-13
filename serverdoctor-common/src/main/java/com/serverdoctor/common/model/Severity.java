package com.serverdoctor.common.model;

/** Schweregrad eines Befunds. Aufsteigend geordnet. */
public enum Severity {
    OK, INFO, LOW, MEDIUM, HIGH, CRITICAL;

    public boolean atLeast(Severity other) {
        return this.ordinal() >= other.ordinal();
    }

    public static Severity max(Severity a, Severity b) {
        return a.ordinal() >= b.ordinal() ? a : b;
    }
}
