package com.serverdoctor.rest;

import java.util.List;

/** Winziger, abhängigkeitsfreier JSON-Writer. Werte sind bereits gerendertes JSON. */
final class J {

    private J() {}

    /** Alternierend: key, rawValue, key, rawValue, ... */
    static String obj(String... kv) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append(s(kv[i])).append(':').append(kv[i + 1]);
        }
        return sb.append('}').toString();
    }

    static String arr(List<String> rawItems) {
        return "[" + String.join(",", rawItems) + "]";
    }

    static String s(String v) {
        if (v == null) return "null";
        StringBuilder sb = new StringBuilder(v.length() + 2).append('"');
        for (int i = 0; i < v.length(); i++) {
            char c = v.charAt(i);
            switch (c) {
                case '"'  -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        return sb.append('"').toString();
    }

    static String n(long v) { return Long.toString(v); }

    static String n(double v) { return Double.isNaN(v) || Double.isInfinite(v) ? "null" : Double.toString(v); }

    static String b(boolean v) { return Boolean.toString(v); }
}
