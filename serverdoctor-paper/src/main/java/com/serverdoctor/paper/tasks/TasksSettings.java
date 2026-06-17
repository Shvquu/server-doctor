package com.serverdoctor.paper.tasks;

/** Automated-task settings, editable in config.yml under {@code tasks}. */
public record TasksSettings(
        boolean enabled,
        long scanIntervalSeconds,
        long scanInitDelaySeconds,
        boolean warnOnHigh
) {

    public static TasksSettings defaults() {
        return new TasksSettings(true, 300L, 30, true);
    }

    public boolean scanEnabled() {
        return enabled;
    }

    public long intervalTicks() {
        return scanIntervalSeconds * 20L;
    }

    public long initDelayTicks() {
        return scanInitDelaySeconds * 20L;
    }

}
