package com.serverdoctor.velocity.platform;

import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.platform.PluginAdapter;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.proxy.ProxyServer;

import java.util.ArrayList;
import java.util.List;

/** Liest die auf dem Proxy geladenen Plugins read-only. */
public final class VelocityPluginAdapter implements PluginAdapter {

    private final ProxyServer proxy;

    public VelocityPluginAdapter(ProxyServer proxy) { this.proxy = proxy; }

    @Override
    public List<PluginInfo> installed() {
        List<PluginInfo> out = new ArrayList<>();
        for (PluginContainer container : proxy.getPluginManager().getPlugins()) {
            PluginDescription d = container.getDescription();
            List<String> hard = new ArrayList<>();
            List<String> soft = new ArrayList<>();
            d.getDependencies().forEach(dep -> (dep.isOptional() ? soft : hard).add(dep.getId()));
            out.add(new PluginInfo(
                    d.getName().orElse(d.getId()),
                    d.getVersion().orElse(""),
                    d.getAuthors(),
                    hard,
                    soft,
                    true));
        }
        return out;
    }
}
