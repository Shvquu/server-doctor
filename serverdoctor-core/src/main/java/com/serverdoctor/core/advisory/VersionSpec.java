package com.serverdoctor.core.advisory;

import com.serverdoctor.common.util.Versions;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches an installed version against an "affected versions" expression.
 *
 * <p>Grammar (comma-separated clauses are ANDed): {@code *} (all), or clauses like
 * {@code <1.4.0}, {@code <=1.3.2}, {@code >=1.0.0}, {@code >0.9}, {@code =1.2.3}.
 * A bare version ({@code 1.2.3}) means an exact match. Version comparison reuses
 * {@link Versions} (tolerant, SemVer-like).
 */
final class VersionSpec {

    private final boolean matchAll;
    private final List<Clause> clauses;

    private VersionSpec(boolean matchAll, List<Clause> clauses) {
        this.matchAll = matchAll;
        this.clauses = clauses;
    }

    static VersionSpec parse(String raw) {
        if (raw == null) return new VersionSpec(false, List.of());
        String s = raw.trim();
        if (s.isEmpty() || s.equals("*")) return new VersionSpec(true, List.of());
        List<Clause> out = new ArrayList<>();
        for (String part : s.split(",")) {
            Clause c = Clause.parse(part.trim());
            if (c != null) out.add(c);
        }
        return new VersionSpec(false, out);
    }

    boolean matches(String version) {
        if (matchAll) return true;
        if (version == null || version.isBlank()) return false; // can't range-match an unknown version
        if (clauses.isEmpty()) return false;
        for (Clause c : clauses) {
            if (!c.test(version)) return false;
        }
        return true;
    }

    private record Clause(String op, String version) {
        static Clause parse(String token) {
            if (token == null || token.isBlank()) return null;
            String t = token.trim();
            for (String op : new String[]{">=", "<=", "==", "=", ">", "<"}) {
                if (t.startsWith(op)) {
                    String v = t.substring(op.length()).trim();
                    return v.isEmpty() ? null : new Clause(op.equals("==") ? "=" : op, v);
                }
            }
            return new Clause("=", t); // bare version => exact
        }

        boolean test(String version) {
            int cmp = Versions.compare(version, this.version);
            return switch (op) {
                case ">=" -> cmp >= 0;
                case "<=" -> cmp <= 0;
                case ">"  -> cmp > 0;
                case "<"  -> cmp < 0;
                default   -> cmp == 0; // "="
            };
        }
    }
}
