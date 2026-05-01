package com.cesarcosmico.skinswitch.item;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkinSlotKeys {

    private final NamespacedKey slots;
    private final NamespacedKey currentIndex;
    private final NamespacedKey originalLore;
    private final NamespacedKey originalName;
    private final NamespacedKey tokenSkin;
    private final NamespacedKey tokenTooltip;
    private final NamespacedKey tooltipSlots;
    private final NamespacedKey originalTooltipStyle;
    private final NamespacedKey ownerUuid;

    public SkinSlotKeys(JavaPlugin plugin) {
        this.slots = new NamespacedKey(plugin, "slots");
        this.currentIndex = new NamespacedKey(plugin, "current_index");
        this.originalLore = new NamespacedKey(plugin, "original_lore");
        this.originalName = new NamespacedKey(plugin, "original_name");
        this.tokenSkin = new NamespacedKey(plugin, "token_skin");
        this.tokenTooltip = new NamespacedKey(plugin, "token_tooltip");
        this.tooltipSlots = new NamespacedKey(plugin, "tooltip_slots");
        this.originalTooltipStyle = new NamespacedKey(plugin, "original_tooltip_style");
        this.ownerUuid = new NamespacedKey(plugin, "owner_uuid");
    }

    public NamespacedKey slots() { return slots; }
    public NamespacedKey currentIndex() { return currentIndex; }
    public NamespacedKey originalLore() { return originalLore; }
    public NamespacedKey originalName() { return originalName; }
    public NamespacedKey tokenSkin() { return tokenSkin; }
    public NamespacedKey tokenTooltip() { return tokenTooltip; }
    public NamespacedKey tooltipSlots() { return tooltipSlots; }
    public NamespacedKey originalTooltipStyle() { return originalTooltipStyle; }
    public NamespacedKey ownerUuid() { return ownerUuid; }
}
