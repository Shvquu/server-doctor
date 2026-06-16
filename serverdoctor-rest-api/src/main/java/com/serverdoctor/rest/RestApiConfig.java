package com.serverdoctor.rest;

public record RestApiConfig(boolean enabled, String host, int port, String token) {

    public static RestApiConfig disabled() {
        return new RestApiConfig(false, "127.0.0.1", 9173, null);
    }

    public boolean requiresAuth() {
        return token != null && !token.isBlank();
    }

}
