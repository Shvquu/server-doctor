package com.serverdoctor.common.model;

/** Statische Eckdaten der Server-Umgebung. */
public record ServerInfo(String platform, String version, String javaVersion) {}
