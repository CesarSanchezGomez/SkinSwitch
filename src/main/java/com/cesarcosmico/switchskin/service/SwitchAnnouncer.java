package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public final class SwitchAnnouncer {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<PluginConfig> pluginSupplier;

    public SwitchAnnouncer(Supplier<LangConfig> langSupplier,
                           Supplier<PluginConfig> pluginSupplier) {
        this.langSupplier = langSupplier;
        this.pluginSupplier = pluginSupplier;
    }

    public void announceSwitch(Player player, SkinDefinition skin) {
        final LangConfig lang = langSupplier.get();
        final PluginConfig.FeedbackConfig feedback = pluginSupplier.get().getSwitchFeedback();
        if (feedback.isActionBar()) {
            lang.sendActionBar(player, "skin.switched", "{skin}", skin.nameOrId());
        } else {
            lang.send(player, "skin.switched", "{skin}", skin.nameOrId());
        }
        playSwitchSound(player);
    }

    public void playSwitchSound(Player player) {
        final PluginConfig.SoundConfig sound = pluginSupplier.get().getSwitchSound();
        if (!sound.playable()) return;
        player.playSound(player.getLocation(), sound.key(), sound.volume(), sound.pitch());
    }
}
