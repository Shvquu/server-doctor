package com.serverdoctor.paper.platform;

import com.serverdoctor.platform.PlayerAdapter;
import org.bukkit.Bukkit;

public final class PaperPlayerAdapter implements PlayerAdapter {
    @Override public int onlineCount() { return Bukkit.getOnlinePlayers().size(); }
    @Override public int maxPlayers() { return Bukkit.getMaxPlayers(); }
}
