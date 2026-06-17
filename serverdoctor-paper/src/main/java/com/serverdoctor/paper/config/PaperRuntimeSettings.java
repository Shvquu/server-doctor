package com.serverdoctor.paper.config;

import com.serverdoctor.paper.gui.GuiSettings;
import com.serverdoctor.paper.tasks.TasksSettings;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

/** Reads the {@code gui} and {@code tasks} sections of config.yml. */
public final class PaperRuntimeSettings {

    private PaperRuntimeSettings() {}

    public static GuiSettings gui(FileConfiguration cfg) {
        ConfigurationSection s = cfg.getConfigurationSection("gui");
        if (s == null) return GuiSettings.defaults();
        return new GuiSettings(s.getBoolean("enabled", true), s.getString("title", "ServerDoctor"));
    }

    public static TasksSettings tasks(FileConfiguration cfg) {
        ConfigurationSection s = cfg.getConfigurationSection("tasks");
        if (s == null) return TasksSettings.defaults();
        ConfigurationSection scan = s.getConfigurationSection("scan");
        if (scan == null) return TasksSettings.defaults();
        return new TasksSettings(
                scan.getBoolean("enabled", true),
                // clamp to a sane minimum so a misconfig can't hammer the server
                Math.max(10L, scan.getLong("interval-seconds", 300L)),
                Math.max(0L, scan.getLong("initial-delay-seconds", 30L)),
                scan.getBoolean("warn-on-high", true));
    }

}
