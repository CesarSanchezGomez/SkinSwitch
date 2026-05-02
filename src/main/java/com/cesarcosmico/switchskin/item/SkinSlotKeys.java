package com.cesarcosmico.switchskin.item;

import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public final class SkinSlotKeys {

    private final NamespacedKey slots;
    private final NamespacedKey currentIndex;
    private final NamespacedKey originalLore;
    private final NamespacedKey originalName;
    private final NamespacedKey originalItemModel;
    private final NamespacedKey tokenSkin;
    private final NamespacedKey tokenTooltip;
    private final NamespacedKey tooltipSlots;
    private final NamespacedKey originalTooltipStyle;
    private final NamespacedKey ownerUuid;

    private final NamespacedKey originalCmdPresent;
    private final NamespacedKey originalCmdFloats;
    private final NamespacedKey originalCmdFlags;
    private final NamespacedKey originalCmdStrings;
    private final NamespacedKey originalCmdColors;

    private final NamespacedKey originalTooltipDisplayPresent;
    private final NamespacedKey originalTooltipDisplayHide;
    private final NamespacedKey originalTooltipDisplayHidden;

    public SkinSlotKeys(JavaPlugin plugin) {
        this.slots = new NamespacedKey(plugin, "slots");
        this.currentIndex = new NamespacedKey(plugin, "current_index");
        this.originalLore = new NamespacedKey(plugin, "original_lore");
        this.originalName = new NamespacedKey(plugin, "original_name");
        this.originalItemModel = new NamespacedKey(plugin, "original_item_model");
        this.tokenSkin = new NamespacedKey(plugin, "token_skin");
        this.tokenTooltip = new NamespacedKey(plugin, "token_tooltip");
        this.tooltipSlots = new NamespacedKey(plugin, "tooltip_slots");
        this.originalTooltipStyle = new NamespacedKey(plugin, "original_tooltip_style");
        this.ownerUuid = new NamespacedKey(plugin, "owner_uuid");

        this.originalCmdPresent = new NamespacedKey(plugin, "original_cmd_present");
        this.originalCmdFloats = new NamespacedKey(plugin, "original_cmd_floats");
        this.originalCmdFlags = new NamespacedKey(plugin, "original_cmd_flags");
        this.originalCmdStrings = new NamespacedKey(plugin, "original_cmd_strings");
        this.originalCmdColors = new NamespacedKey(plugin, "original_cmd_colors");

        this.originalTooltipDisplayPresent = new NamespacedKey(plugin, "original_tooltip_display_present");
        this.originalTooltipDisplayHide = new NamespacedKey(plugin, "original_tooltip_display_hide");
        this.originalTooltipDisplayHidden = new NamespacedKey(plugin, "original_tooltip_display_hidden");
    }

    public NamespacedKey slots() { return slots; }
    public NamespacedKey currentIndex() { return currentIndex; }
    public NamespacedKey originalLore() { return originalLore; }
    public NamespacedKey originalName() { return originalName; }
    public NamespacedKey originalItemModel() { return originalItemModel; }
    public NamespacedKey tokenSkin() { return tokenSkin; }
    public NamespacedKey tokenTooltip() { return tokenTooltip; }
    public NamespacedKey tooltipSlots() { return tooltipSlots; }
    public NamespacedKey originalTooltipStyle() { return originalTooltipStyle; }
    public NamespacedKey ownerUuid() { return ownerUuid; }

    public NamespacedKey originalCmdPresent() { return originalCmdPresent; }
    public NamespacedKey originalCmdFloats() { return originalCmdFloats; }
    public NamespacedKey originalCmdFlags() { return originalCmdFlags; }
    public NamespacedKey originalCmdStrings() { return originalCmdStrings; }
    public NamespacedKey originalCmdColors() { return originalCmdColors; }

    public NamespacedKey originalTooltipDisplayPresent() { return originalTooltipDisplayPresent; }
    public NamespacedKey originalTooltipDisplayHide() { return originalTooltipDisplayHide; }
    public NamespacedKey originalTooltipDisplayHidden() { return originalTooltipDisplayHidden; }
}
