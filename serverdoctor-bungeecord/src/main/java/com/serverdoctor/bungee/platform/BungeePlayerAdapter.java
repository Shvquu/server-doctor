package com.serverdoctor.bungee.platform;

import com.serverdoctor.platform.PlayerAdapter;
import net.md_5.bungee.api.ProxyServer;

public final class BungeePlayerAdapter implements PlayerAdapter {

    private final ProxyServer proxy;

    public BungeePlayerAdapter(ProxyServer proxy) { this.proxy = proxy; }

    @Override public int onlineCount() { return proxy.getOnlineCount(); }
    @Override public int maxPlayers() { return proxy.getConfig().getPlayerLimit(); }
}
