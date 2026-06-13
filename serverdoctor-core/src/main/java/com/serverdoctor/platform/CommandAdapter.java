package com.serverdoctor.platform;

/** Abstraktion für Command-Registrierung (read-only Diagnose-Befehle). */
public interface CommandAdapter {

    void register(String name, Handler handler);

    interface Handler {
        /** @return Ausgabezeilen, die der Aufrufer erhält. */
        java.util.List<String> handle(String[] args);
    }
}
