package com.serverdoctor.api;

import com.serverdoctor.api.exception.ApiNotInitializedException;

/** Statischer Service-Locator. Wird beim Plugin-Start gesetzt. */
public final class ServerDoctorProvider {

    private static volatile ServerDoctorApi instance;

    private ServerDoctorProvider() {}

    public static ServerDoctorApi get() {
        ServerDoctorApi api = instance;
        if (api == null) {
            throw new ApiNotInitializedException("ServerDoctor ist noch nicht initialisiert.");
        }
        return api;
    }

    public static boolean isAvailable() { return instance != null; }

    /** Intern: wird vom Plattform-Bootstrap aufgerufen. */
    public static void register(ServerDoctorApi api) { instance = api; }

    public static void unregister() { instance = null; }
}
