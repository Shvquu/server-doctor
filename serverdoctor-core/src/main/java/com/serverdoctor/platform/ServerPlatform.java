package com.serverdoctor.platform;

import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.ServerInfo;

import java.util.Set;

/** Bündelt alle plattformabhängigen Adapter hinter einer Fassade. */
public interface ServerPlatform {

    String name();

    ServerInfo serverInfo();

    Set<Capability> capabilities();

    PluginAdapter plugins();

    PlayerAdapter players();

    MetricsAdapter metrics();

    SchedulerAdapter scheduler();

    LoggerAdapter logger();

    CommandAdapter commands();
}
