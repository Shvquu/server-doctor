package com.serverdoctor.paper.gui;

/** GUI settings, editable in config.yml under {@code gui}. */
public record GuiSettings(boolean enabled, String title) {

    public static GuiSettings defaults() {
        return new GuiSettings(true, "ServerDoctor");
    }

}
