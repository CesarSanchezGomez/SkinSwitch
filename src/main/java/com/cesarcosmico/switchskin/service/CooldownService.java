package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.PluginConfig;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class CooldownService {

    private final Supplier<PluginConfig> pluginSupplier;
    private final ConcurrentHashMap<UUID, Long> nextAllowed = new ConcurrentHashMap<>();

    public CooldownService(Supplier<PluginConfig> pluginSupplier) {
        this.pluginSupplier = pluginSupplier;
    }

    public boolean isReady(Player player) {
        return remainingMillis(player) <= 0;
    }

    public long remainingMillis(Player player) {
        final PluginConfig.CooldownConfig cd = pluginSupplier.get().getSwitchCooldown();
        if (!cd.active()) return 0;
        final Long until = nextAllowed.get(player.getUniqueId());
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }

    public void mark(Player player) {
        final PluginConfig.CooldownConfig cd = pluginSupplier.get().getSwitchCooldown();
        if (!cd.active()) return;
        nextAllowed.put(player.getUniqueId(), System.currentTimeMillis() + cd.millis());
    }

    public void clear(Player player) {
        nextAllowed.remove(player.getUniqueId());
    }
}
