package com.cesarcosmico.switchskin.service;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerLockService implements Listener {

    private final Set<UUID> locked = ConcurrentHashMap.newKeySet();

    public boolean isLocked(Player player) {
        return locked.contains(player.getUniqueId());
    }

    public boolean toggle(Player player) {
        final UUID id = player.getUniqueId();
        if (locked.add(id)) return true;
        locked.remove(id);
        return false;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        locked.remove(event.getPlayer().getUniqueId());
    }
}
