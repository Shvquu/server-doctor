package com.serverdoctor.paper.platform;

import com.serverdoctor.common.model.PluginInfo;
import com.serverdoctor.platform.PluginAdapter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;

import java.util.ArrayList;
import java.util.List;

/** Liest installierte Plugins read-only aus dem Bukkit-PluginManager. */
public final class PaperPluginAdapter implements PluginAdapter {

    @Override
    public List<PluginInfo> installed() {
        List<PluginInfo> out = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            PluginDescriptionFile d = plugin.getDescription();
            out.add(new PluginInfo(
                    d.getName(),
                    d.getVersion(),
                    d.getAuthors(),
                    d.getDepend(),
                    d.getSoftDepend(),
                    plugin.isEnabled(),
                    d.getAPIVersion()));      // api-version from plugin.yml (null if not set)
        }
        return out;
    }
}
