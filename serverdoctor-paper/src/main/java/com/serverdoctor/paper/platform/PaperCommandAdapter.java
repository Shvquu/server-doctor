package com.serverdoctor.paper.platform;

import com.serverdoctor.platform.CommandAdapter;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/** Registriert read-only Diagnose-Commands aus der plugin.yml. */
public final class PaperCommandAdapter implements CommandAdapter {

    private final JavaPlugin plugin;

    public PaperCommandAdapter(JavaPlugin plugin) { this.plugin = plugin; }

    @Override
    public void register(String name, Handler handler) {
        PluginCommand command = plugin.getCommand(name);
        if (command == null) {
            plugin.getLogger().warning("Command '" + name + "' nicht in plugin.yml deklariert.");
            return;
        }
        command.setExecutor((CommandSender sender, org.bukkit.command.Command cmd, String label, String[] args) -> {
            for (String line : handler.handle(args)) sender.sendMessage(line);
            return true;
        });
    }
}
