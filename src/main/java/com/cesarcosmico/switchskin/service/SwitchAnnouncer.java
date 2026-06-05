package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.text.MessageManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;

public final class SwitchAnnouncer {

    private static final String VANILLA_KEY = "skin.switched-vanilla";
    private static final String SKIN_KEY = "skin.switched";

    private final MessageManager messages;
    private final PluginConfig pluginConfig;

    public SwitchAnnouncer(MessageManager messages, PluginConfig pluginConfig) {
        this.messages = messages;
        this.pluginConfig = pluginConfig;
    }

    public void announceSwitch(Player player, SkinDefinition skin) {
        announce(player, messages.getPrefixedMessage(SKIN_KEY, Placeholder.parsed("skin", skin.nameOrId())));
    }

    public void announceVanilla(Player player) {
        announce(player, messages.getPrefixedMessage(VANILLA_KEY));
    }

    public void playTokenSound(Player player) {
        play(player, pluginConfig.getTokenSound());
    }

    public void playSwitchSound(Player player) {
        play(player, pluginConfig.getSwitchSound());
    }

    private void announce(Player player, Component message) {
        if (pluginConfig.getSwitchFeedback().isActionBar()) {
            player.sendActionBar(message);
        } else {
            player.sendMessage(message);
        }
        playSwitchSound(player);
    }

    private static void play(Player player, PluginConfig.SoundConfig sound) {
        if (!sound.playable()) return;
        player.playSound(player.getLocation(), sound.key(), sound.volume(), sound.pitch());
    }
}
