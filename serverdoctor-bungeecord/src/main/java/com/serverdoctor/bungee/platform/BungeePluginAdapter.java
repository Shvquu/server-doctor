package com.serverdoctor.bungee.platform;

import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.platform.PluginAdapter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.plugin.PluginDescription;

import java.util.ArrayList;
import java.util.List;

/** Reads the proxy's loaded plugins, read-only. */
public final class BungeePluginAdapter implements PluginAdapter {

    private final ProxyServer proxy;

    public BungeePluginAdapter(ProxyServer proxy) { this.proxy = proxy; }

    @Override
    public List<PluginInfo> installed() {
        List<PluginInfo> out = new ArrayList<>();
        for (Plugin plugin : proxy.getPluginManager().getPlugins()) {
            PluginDescription d = plugin.getDescription();
            String author = d.getAuthor();
            List<String> authors = author == null || author.isBlank() ? List.of() : List.of(author);
            out.add(new PluginInfo(
                    d.getName(),
                    d.getVersion() == null ? "" : d.getVersion(),
                    authors,
                    new ArrayList<>(d.getDepends()),
                    new ArrayList<>(d.getSoftDepends()),
                    true));
        }
        return out;
    }
}
