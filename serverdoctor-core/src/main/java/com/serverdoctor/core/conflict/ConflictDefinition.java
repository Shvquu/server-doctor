package com.serverdoctor.core.conflict;

import com.serverdoctor.common.model.Severity;

/** Erweiterbare Definition eines bekannten Konflikts zweier Plugins. */
public record ConflictDefinition(String id, String pluginA, String pluginB,
                                 Severity severity, String description) {}
