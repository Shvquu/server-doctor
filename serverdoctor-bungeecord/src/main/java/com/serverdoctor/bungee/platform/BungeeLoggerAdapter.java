package com.serverdoctor.bungee.platform;

import com.serverdoctor.platform.LoggerAdapter;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class BungeeLoggerAdapter implements LoggerAdapter {

    private final Logger logger;

    public BungeeLoggerAdapter(Logger logger) { this.logger = logger; }

    @Override public void info(String message) { logger.info(message); }
    @Override public void warn(String message) { logger.warning(message); }
    @Override public void error(String message, Throwable throwable) { logger.log(Level.SEVERE, message, throwable); }
}
