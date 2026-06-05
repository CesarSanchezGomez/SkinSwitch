package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.adapter.placeholderapi.PlaceholderResolver;
import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.config.TooltipDisplayConfig;
import com.cesarcosmico.switchskin.domain.id.SkinId;
import com.cesarcosmico.switchskin.domain.model.AppearanceSnapshot;
import com.cesarcosmico.switchskin.domain.model.SkinLoadout;
import com.cesarcosmico.switchskin.items.LoreRenderer;
import com.cesarcosmico.switchskin.items.TooltipDisplaySupport;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/** Applies the active skin's look (or the captured original) to a held item. */
public final class SkinAppearanceRenderer {

    private static final MiniMessage MINI = MiniMessage.miniMessage();
    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private final SkinStateCodec codec;
    private final LoreRenderer loreRenderer;
    private final SkinConfig skinConfig;
    private final PlaceholderResolver placeholderResolver;
    private final Logger logger;

    public SkinAppearanceRenderer(SkinStateCodec codec, LoreRenderer loreRenderer,
                                  SkinConfig skinConfig, PlaceholderResolver placeholderResolver,
                                  Logger logger) {
        this.codec = codec;
        this.loreRenderer = loreRenderer;
        this.skinConfig = skinConfig;
        this.placeholderResolver = placeholderResolver;
        this.logger = logger;
    }

    public void render(ItemStack item, ItemMeta meta, SkinLoadout loadout) {
        final AppearanceSnapshot snapshot = codec.readAppearance(meta);
        final OfflinePlayer owner = resolveOwner(meta);
        final Optional<SkinId> active = loadout.activeSkin();
        final SkinDefinition activeSkin = active
                .flatMap(id -> skinConfig.get(id.value()))
                .orElse(null);

        if (activeSkin != null) {
            applySkinAppearance(meta, snapshot, activeSkin, owner);
        } else {
            applyOriginalModel(meta, snapshot);
            applyOriginalName(meta, snapshot);
            applyOriginalCustomModelData(meta, snapshot);
        }

        applyTooltipStyle(meta, snapshot, loadout, activeSkin);
        applyLore(meta, snapshot, loadout, activeSkin, owner);
        item.setItemMeta(meta);
        applyTooltipDisplay(item, snapshot, activeSkin);
    }

    public void restoreOriginal(ItemStack item, ItemMeta meta, AppearanceSnapshot snapshot) {
        applyOriginalModel(meta, snapshot);
        applyOriginalName(meta, snapshot);
        applyOriginalCustomModelData(meta, snapshot);
        applyOriginalTooltipStyle(meta, snapshot);
        applyOriginalLore(meta, snapshot);
        item.setItemMeta(meta);
        applyOriginalTooltipDisplay(item, snapshot);
    }

    /** Applies a skin's model + custom_model_data preview to a pre-built menu icon. */
    public void preview(ItemStack item, @Nullable SkinDefinition skin) {
        if (skin == null) return;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (skin.itemModel() != null && meta.getItemModel() == null) {
            meta.setItemModel(skin.itemModel());
        }
        item.setItemMeta(meta);
        if (skin.customModelData() != null) {
            applyCustomModelData(item, skin.customModelData());
        }
    }

    private void applySkinAppearance(ItemMeta meta, AppearanceSnapshot snapshot,
                                     SkinDefinition skin, @Nullable OfflinePlayer owner) {
        if (skin.itemModel() != null) {
            meta.setItemModel(skin.itemModel());
        } else {
            applyOriginalModel(meta, snapshot);
        }

        if (skin.hasName()) {
            final String resolved = placeholderResolver.resolve(owner, skin.name());
            meta.displayName(MINI.deserialize(resolved).decoration(TextDecoration.ITALIC, false));
        } else {
            applyOriginalName(meta, snapshot);
        }

        if (skin.customModelData() != null) {
            applyCustomModelDataOnMeta(meta, skin.customModelData());
        } else {
            applyOriginalCustomModelData(meta, snapshot);
        }
    }

    private void applyTooltipStyle(ItemMeta meta, AppearanceSnapshot snapshot,
                                   SkinLoadout loadout, @Nullable SkinDefinition activeSkin) {
        final boolean activeHasTooltip = activeSkin != null
                && loadout.activeSkin().map(loadout::hasTooltip).orElse(false);
        if (activeHasTooltip && activeSkin.tooltipStyle() != null) {
            meta.setTooltipStyle(activeSkin.tooltipStyle());
        } else {
            applyOriginalTooltipStyle(meta, snapshot);
        }
    }

    private void applyTooltipDisplay(ItemStack item, AppearanceSnapshot snapshot,
                                     @Nullable SkinDefinition activeSkin) {
        if (activeSkin != null && activeSkin.tooltipDisplay() != null) {
            TooltipDisplaySupport.applyTo(item, activeSkin.tooltipDisplay());
        } else {
            applyOriginalTooltipDisplay(item, snapshot);
        }
    }

    private void applyLore(ItemMeta meta, AppearanceSnapshot snapshot, SkinLoadout loadout,
                           @Nullable SkinDefinition activeSkin, @Nullable OfflinePlayer owner) {
        final List<Component> baseLore = resolveBaseLore(snapshot, activeSkin, owner);
        meta.lore(loreRenderer.render(baseLore, idStrings(loadout), loadout.activeIndex(),
                tooltipStrings(loadout), owner));
    }

    private List<Component> resolveBaseLore(AppearanceSnapshot snapshot,
                                            @Nullable SkinDefinition activeSkin,
                                            @Nullable OfflinePlayer owner) {
        if (activeSkin != null && activeSkin.hasLore()) {
            return activeSkin.lore().stream()
                    .map(line -> placeholderResolver.resolve(owner, line))
                    .<Component>map(line -> MINI.deserialize(line).decoration(TextDecoration.ITALIC, false))
                    .toList();
        }
        return snapshot.loreJson().stream().<Component>map(GSON::deserialize).toList();
    }

    private void applyOriginalModel(ItemMeta meta, AppearanceSnapshot snapshot) {
        final String raw = snapshot.itemModel();
        meta.setItemModel(raw != null && !raw.isEmpty() ? NamespacedKey.fromString(raw) : null);
    }

    private void applyOriginalName(ItemMeta meta, AppearanceSnapshot snapshot) {
        final String json = snapshot.nameJson();
        meta.displayName(json != null ? GSON.deserialize(json) : null);
    }

    private void applyOriginalTooltipStyle(ItemMeta meta, AppearanceSnapshot snapshot) {
        final String raw = snapshot.tooltipStyle();
        meta.setTooltipStyle(raw != null && !raw.isEmpty() ? NamespacedKey.fromString(raw) : null);
    }

    private void applyOriginalLore(ItemMeta meta, AppearanceSnapshot snapshot) {
        final List<String> serialized = snapshot.loreJson();
        if (serialized.isEmpty()) {
            meta.lore(null);
        } else {
            meta.lore(serialized.stream().map(GSON::deserialize).toList());
        }
    }

    private void applyOriginalCustomModelData(ItemMeta meta, AppearanceSnapshot snapshot) {
        final CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        cmd.setFloats(List.of());
        cmd.setFlags(List.of());
        cmd.setStrings(List.of());
        cmd.setColors(List.of());
        final CustomModelDataConfig original = snapshot.customModelData();
        if (original != null) {
            if (!original.floats().isEmpty()) cmd.setFloats(original.floats());
            if (!original.flags().isEmpty()) cmd.setFlags(original.flags());
            if (!original.strings().isEmpty()) cmd.setStrings(original.strings());
            if (!original.colors().isEmpty()) {
                cmd.setColors(original.colors().stream().map(Color::fromRGB).toList());
            }
        }
        meta.setCustomModelDataComponent(cmd);
    }

    private void applyOriginalTooltipDisplay(ItemStack item, AppearanceSnapshot snapshot) {
        final TooltipDisplayConfig original = snapshot.tooltipDisplay();
        if (original == null) {
            TooltipDisplaySupport.clear(item);
        } else {
            TooltipDisplaySupport.applyTo(item, original);
        }
    }

    private void applyCustomModelDataOnMeta(ItemMeta meta, CustomModelDataConfig cmd) {
        final CustomModelDataComponent comp = meta.getCustomModelDataComponent();
        comp.setFloats(cmd.floats());
        comp.setFlags(cmd.flags());
        comp.setStrings(cmd.strings());
        comp.setColors(cmd.colors().stream().map(Color::fromRGB).toList());
        meta.setCustomModelDataComponent(comp);
    }

    private void applyCustomModelData(ItemStack item, CustomModelDataConfig config) {
        if (config.isEmpty()) return;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        applyCustomModelDataOnMeta(meta, config);
        item.setItemMeta(meta);
    }

    @Nullable
    private OfflinePlayer resolveOwner(ItemMeta meta) {
        return codec.readOwner(meta)
                .map(id -> org.bukkit.Bukkit.getOfflinePlayer(id.value()))
                .orElse(null);
    }

    private static List<String> idStrings(SkinLoadout loadout) {
        return loadout.slots().stream().map(SkinId::value).toList();
    }

    private static List<String> tooltipStrings(SkinLoadout loadout) {
        return loadout.tooltips().stream().map(SkinId::value).toList();
    }
}
