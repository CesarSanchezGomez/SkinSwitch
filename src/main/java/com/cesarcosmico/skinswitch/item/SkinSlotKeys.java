package com.cesarcosmico.skinswitch.item;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkinSlotKeys {

    private final NamespacedKey slots;
    private final NamespacedKey currentIndex;
    private final NamespacedKey originalLore;
    private final NamespacedKey tokenSkin;

    public SkinSlotKeys(JavaPlugin plugin) {
        this.slots = new NamespacedKey(plugin, "slots");
        this.currentIndex = new NamespacedKey(plugin, "current_index");
        this.originalLore = new NamespacedKey(plugin, "original_lore");
        this.tokenSkin = new NamespacedKey(plugin, "token_skin");
    }

    public NamespacedKey slots() { return slots; }
    public NamespacedKey currentIndex() { return currentIndex; }
    public NamespacedKey originalLore() { return originalLore; }
    public NamespacedKey tokenSkin() { return tokenSkin; }
}
