package com.serverdoctor.velocity.platform;

import com.serverdoctor.platform.PlayerAdapter;
import com.velocitypowered.api.proxy.ProxyServer;

public final class VelocityPlayerAdapter implements PlayerAdapter {

    private final ProxyServer proxy;

    public VelocityPlayerAdapter(ProxyServer proxy) { this.proxy = proxy; }

    @Override public int onlineCount() { return proxy.getPlayerCount(); }
    @Override public int maxPlayers() { return proxy.getConfiguration().getShowMaxPlayers(); }
}
