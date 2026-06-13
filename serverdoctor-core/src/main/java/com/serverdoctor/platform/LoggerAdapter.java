package com.serverdoctor.platform;

public interface LoggerAdapter {
    void info(String message);
    void warn(String message);
    void error(String message, Throwable throwable);
}
