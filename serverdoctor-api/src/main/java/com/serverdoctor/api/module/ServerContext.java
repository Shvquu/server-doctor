package com.serverdoctor.api.module;

import com.serverdoctor.common.model.Capability;
import com.serverdoctor.common.model.PerformanceSnapshot;
import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.common.model.ServerInfo;

import java.util.List;
import java.util.Set;

/**
 * Read-only-Facade, die jedem {@link AnalysisModule} übergeben wird.
 * Exponiert bewusst KEINE plattformabhängigen oder schreibenden Operationen.
 */
public interface ServerContext {

    Set<Capability> capabilities();

    boolean has(Capability capability);

    List<PluginInfo> plugins();

    PerformanceSnapshot performance();

    ServerInfo serverInfo();
}
