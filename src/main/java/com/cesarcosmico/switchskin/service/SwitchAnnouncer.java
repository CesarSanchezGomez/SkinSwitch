package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import org.bukkit.entity.Player;

import java.util.function.Supplier;

public final class SwitchAnnouncer {

    private static final String VANILLA_KEY = "skin.switched-vanilla";
    private static final String SKIN_KEY = "skin.switched";

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<PluginConfig> pluginSupplier;

    public SwitchAnnouncer(Supplier<LangConfig> langSupplier,
                           Supplier<PluginConfig> pluginSupplier) {
        this.langSupplier = langSupplier;
        this.pluginSupplier = pluginSupplier;
    }

    public void announceSwitch(Player player, SkinDefinition skin) {
        announce(player, SKIN_KEY, "{skin}", skin.nameOrId());
    }

    public void announceVanilla(Player player) {
        announce(player, VANILLA_KEY);
    }

    public void playTokenSound(Player player) {
        play(player, pluginSupplier.get().getTokenSound());
    }

    private void announce(Player player, String key, String... placeholders) {
        final LangConfig lang = langSupplier.get();
        if (pluginSupplier.get().getSwitchFeedback().isActionBar()) {
            lang.sendActionBar(player, key, placeholders);
        } else {
            lang.send(player, key, placeholders);
        }
        playSwitchSound(player);
    }

    public void playSwitchSound(Player player) {
        play(player, pluginSupplier.get().getSwitchSound());
    }

    private static void play(Player player, PluginConfig.SoundConfig sound) {
        if (!sound.playable()) return;
        player.playSound(player.getLocation(), sound.key(), sound.volume(), sound.pitch());
    }
}
