package com.serverdoctor.common.model;

/**
 * Plattform-Fähigkeiten. Scanner deklarieren, welche sie benötigen;
 * die Engine überspringt nicht-anwendbare Module.
 */
public enum Capability {
    HAS_PLUGINS,
    HAS_TICK_LOOP,
    HAS_ENTITIES,
    HAS_REGIONS,
    IS_PROXY
}
