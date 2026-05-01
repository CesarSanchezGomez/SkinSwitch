package com.cesarcosmico.switchskin.gui;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class SkinMenuGUI implements InventoryHolder {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Inventory inventory;
    private final List<String> skinIds;
    private final int activeIndex;

    public SkinMenuGUI(LangConfig lang, SkinConfig skinConfig, PluginConfig pluginConfig,
                       List<String> skinIds, int activeIndex) {
        this.skinIds = List.copyOf(skinIds);
        this.activeIndex = activeIndex;

        final PluginConfig.MenuConfig cfg = pluginConfig.getMenu();
        final int size = cfg.rows() * 9;
        final Component title = MINI.deserialize(cfg.title());
        this.inventory = Bukkit.createInventory(this, size, title);

        populate(lang, skinConfig);
    }

    public void open(Player player) {
        player.openInventory(inventory);
    }

    public List<String> getSkinIds() {
        return skinIds;
    }

    public int getActiveIndex() {
        return activeIndex;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private void populate(LangConfig lang, SkinConfig skinConfig) {
        for (int i = 0; i < skinIds.size() && i < inventory.getSize(); i++) {
            final String id = skinIds.get(i);
            final SkinDefinition def = skinConfig.get(id).orElse(null);
            inventory.setItem(i, buildEntry(lang, id, def, i == activeIndex));
        }
    }

    private ItemStack buildEntry(LangConfig lang, String skinId, SkinDefinition def, boolean active) {
        final ItemStack item = new ItemStack(Material.NAME_TAG);
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        final String displayName = def != null ? def.nameOrId() : skinId;
        final String nameKey = active ? "menu.entry-active-name" : "menu.entry-inactive-name";
        meta.displayName(deserialize(lang.getRaw(nameKey).replace("{skin}", displayName)));

        final String loreKey = active ? "menu.entry-active-lore" : "menu.entry-inactive-lore";
        final List<String> rawLore = lang.getRawList(loreKey);
        if (!rawLore.isEmpty()) {
            final List<Component> lore = new ArrayList<>(rawLore.size());
            for (String line : rawLore) {
                lore.add(deserialize(line.replace("{skin}", displayName)));
            }
            meta.lore(lore);
        }

        if (def != null) {
            meta.setItemModel(def.itemModel());
        }
        item.setItemMeta(meta);
        return item;
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
