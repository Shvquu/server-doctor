package com.serverdoctor.core.compat;

import java.time.LocalDate;

/**
 * Optional, externally-sourced metadata about a plugin's maintenance state. Always from a real
 * feed - never fabricated. Any field may be absent.
 *
 * @param lastUpdated     release date of the latest version, or {@code null} if unknown
 * @param foliaSupported  tri-state: {@code TRUE}/{@code FALSE}/{@code null} (unknown)
 * @param note            short maintenance / known-incompatibility note (may be empty)
 * @param reference       link to the source (may be empty)
 */
public record CompatMetadata(String pluginName, LocalDate lastUpdated,
                             Boolean foliaSupported, String note, String reference) {}
