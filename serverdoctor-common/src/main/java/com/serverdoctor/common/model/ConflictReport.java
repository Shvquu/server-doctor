package com.serverdoctor.common.model;

/** Erkannter (potenzieller) Konflikt zwischen zwei Plugins. */
public record ConflictReport(String id, String pluginA, String pluginB,
                             Severity severity, String description) {}
