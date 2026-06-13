package com.serverdoctor.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Hilfsklasse für simplen, toleranten Versionsvergleich (SemVer-ähnlich). */
public final class Versions {

    private static final Pattern NUM = Pattern.compile("\\d+");

    private Versions() {}

    /** -1 wenn a &lt; b, 0 bei Gleichheit, 1 wenn a &gt; b. Tolerant gegenüber Suffixen. */
    public static int compare(String a, String b) {
        int[] pa = parse(a);
        int[] pb = parse(b);
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? pa[i] : 0;
            int vb = i < pb.length ? pb[i] : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    public static boolean isNewer(String a, String b) { return compare(a, b) > 0; }
    public static boolean isOlder(String a, String b) { return compare(a, b) < 0; }

    private static int[] parse(String v) {
        if (v == null || v.isBlank()) return new int[]{0};
        Matcher m = NUM.matcher(v);
        java.util.List<Integer> parts = new java.util.ArrayList<>();
        while (m.find()) parts.add(Integer.parseInt(m.group()));
        if (parts.isEmpty()) return new int[]{0};
        return parts.stream().mapToInt(Integer::intValue).toArray();
    }
}
