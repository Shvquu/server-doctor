package com.serverdoctor.common.model;

/** Ein einzelner Befund eines Scanners. */
public record Finding(String scannerId, Severity severity, String message) {}
