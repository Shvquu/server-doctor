package com.serverdoctor.core.advisory;

import com.serverdoctor.common.model.Severity;

/**
 * A single security advisory affecting a plugin version. This data always originates from a
 * real, external feed - ServerDoctor never fabricates advisories.
 */
public record Advisory(String id, Severity severity, String summary, String reference) {

    /** Human-readable one-liner for a {@code SecurityRisk} description. */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        if (id != null && !id.isBlank()) sb.append('[').append(id).append("] ");
        sb.append(summary == null || summary.isBlank() ? "Security advisory" : summary);
        if (reference != null && !reference.isBlank()) sb.append(" (").append(reference).append(')');
        return sb.toString();
    }
}
