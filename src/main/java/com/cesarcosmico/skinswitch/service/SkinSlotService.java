package com.cesarcosmico.skinswitch.service;

import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.item.LoreRenderer;
import com.cesarcosmico.skinswitch.item.SkinSlotKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Single source of truth for skin-slot mutations on items.
 *
 * The skin-slot system manages item_model + custom_name + lore. The
 * tooltip_style is tracked independently via the tooltip-token system
 * so that applying/removing skins never touches the tooltip.
 */
public final class SkinSlotService {

    public enum AddResult { ADDED, UNKNOWN_SKIN, FULL, DUPLICATE, NO_META }
    public enum RemoveResult { REMOVED, INVALID_INDEX, NO_SLOTS, NO_META }
    public enum CycleResult { CYCLED, NO_SLOTS, NO_META, SINGLE_SLOT }
    public enum TooltipApplyResult { APPLIED, UNKNOWN_SKIN, NO_TOOLTIP, NO_META }
    public enum TooltipRemoveResult { REMOVED, NOT_APPLIED, NO_META }

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SkinSlotKeys keys;
    private final LoreRenderer loreRenderer;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<PluginConfig> pluginSupplier;

    public SkinSlotService(SkinSlotKeys keys, LoreRenderer loreRenderer,
                           Supplier<SkinConfig> skinSupplier,
                           Supplier<PluginConfig> pluginSupplier) {
        this.keys = keys;
        this.loreRenderer = loreRenderer;
        this.skinSupplier = skinSupplier;
        this.pluginSupplier = pluginSupplier;
    }

    public boolean hasSlots(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keys.slots(), PersistentDataType.LIST.strings());
    }

    public List<String> getSlots(ItemStack item) {
        if (item == null) return List.of();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return List.of();
        List<String> data = meta.getPersistentDataContainer()
                .get(keys.slots(), PersistentDataType.LIST.strings());
        return data == null ? List.of() : data;
    }

    public int getCurrentIndex(ItemStack item) {
        if (item == null) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer idx = meta.getPersistentDataContainer().get(keys.currentIndex(), PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    public Optional<SkinDefinition> getActiveSkin(ItemStack item) {
        List<String> slots = getSlots(item);
        if (slots.isEmpty()) return Optional.empty();
        int idx = getCurrentIndex(item);
        if (idx < 0 || idx >= slots.size()) return Optional.empty();
        return skinSupplier.get().get(slots.get(idx));
    }

    public boolean hasTooltip(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keys.appliedTooltipSkin(), PersistentDataType.STRING);
    }

    public AddResult addSlot(ItemStack item, String skinId) {
        if (item == null) return AddResult.NO_META;
        Optional<SkinDefinition> skinOpt = skinSupplier.get().get(skinId);
        if (skinOpt.isEmpty()) return AddResult.UNKNOWN_SKIN;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return AddResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<String> current = readSlots(pdc);

        if (current.size() >= pluginSupplier.get().getDefaultMaxSlots()) return AddResult.FULL;
        if (current.contains(skinId)) return AddResult.DUPLICATE;

        boolean firstSlot = current.isEmpty();
        if (firstSlot) {
            captureOriginalAppearance(meta, pdc);
        }

        SkinDefinition skin = skinOpt.get();
        current.add(skinId);
        pdc.set(keys.slots(), PersistentDataType.LIST.strings(), current);

        int currentIndex = readIndex(pdc);
        if (firstSlot) {
            currentIndex = 0;
            pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, 0);
            applySkinAppearance(meta, pdc, skin);
        }

        applyLore(meta, current, currentIndex);
        item.setItemMeta(meta);
        return AddResult.ADDED;
    }

    public RemoveResult removeSlot(ItemStack item, int index) {
        if (item == null) return RemoveResult.NO_META;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return RemoveResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) return RemoveResult.NO_SLOTS;

        List<String> current = readSlots(pdc);
        if (index < 0 || index >= current.size()) return RemoveResult.INVALID_INDEX;

        current.remove(index);
        int currentIndex = readIndex(pdc);

        if (current.isEmpty()) {
            restoreOriginalAppearance(meta, pdc);
            pdc.remove(keys.slots());
            pdc.remove(keys.currentIndex());
        } else {
            if (currentIndex >= current.size()) currentIndex = 0;
            pdc.set(keys.slots(), PersistentDataType.LIST.strings(), current);
            pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, currentIndex);
            int finalIndex = currentIndex;
            skinSupplier.get().get(current.get(finalIndex))
                    .ifPresent(s -> applySkinAppearance(meta, pdc, s));
            applyLore(meta, current, currentIndex);
        }
        item.setItemMeta(meta);
        return RemoveResult.REMOVED;
    }

    public CycleResult cycleNext(ItemStack item) {
        if (item == null) return CycleResult.NO_META;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return CycleResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) return CycleResult.NO_SLOTS;

        List<String> current = readSlots(pdc);
        if (current.isEmpty()) return CycleResult.NO_SLOTS;
        if (current.size() == 1) return CycleResult.SINGLE_SLOT;

        int next = (readIndex(pdc) + 1) % current.size();
        pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, next);
        skinSupplier.get().get(current.get(next))
                .ifPresent(s -> applySkinAppearance(meta, pdc, s));
        applyLore(meta, current, next);
        item.setItemMeta(meta);
        return CycleResult.CYCLED;
    }

    public TooltipApplyResult applyTooltip(ItemStack item, String skinId) {
        if (item == null) return TooltipApplyResult.NO_META;
        Optional<SkinDefinition> skinOpt = skinSupplier.get().get(skinId);
        if (skinOpt.isEmpty()) return TooltipApplyResult.UNKNOWN_SKIN;
        SkinDefinition skin = skinOpt.get();
        if (skin.tooltipStyle() == null) return TooltipApplyResult.NO_TOOLTIP;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipApplyResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.appliedTooltipSkin(), PersistentDataType.STRING)) {
            captureOriginalTooltipStyle(meta, pdc);
        }
        pdc.set(keys.appliedTooltipSkin(), PersistentDataType.STRING, skinId);
        meta.setTooltipStyle(skin.tooltipStyle());
        item.setItemMeta(meta);
        return TooltipApplyResult.APPLIED;
    }

    public TooltipRemoveResult removeTooltip(ItemStack item) {
        if (item == null) return TooltipRemoveResult.NO_META;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipRemoveResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.appliedTooltipSkin(), PersistentDataType.STRING)) {
            return TooltipRemoveResult.NOT_APPLIED;
        }
        restoreOriginalTooltipStyle(meta, pdc);
        pdc.remove(keys.appliedTooltipSkin());
        item.setItemMeta(meta);
        return TooltipRemoveResult.REMOVED;
    }

    private List<String> readSlots(PersistentDataContainer pdc) {
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) {
            return new ArrayList<>();
        }
        List<String> stored = pdc.get(keys.slots(), PersistentDataType.LIST.strings());
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    private int readIndex(PersistentDataContainer pdc) {
        Integer idx = pdc.get(keys.currentIndex(), PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    // ---------- capture / restore ----------

    private void captureOriginalAppearance(ItemMeta meta, PersistentDataContainer pdc) {
        captureOriginalLore(meta, pdc);
        captureOriginalName(meta, pdc);
    }

    private void restoreOriginalAppearance(ItemMeta meta, PersistentDataContainer pdc) {
        restoreOriginalLore(meta, pdc);
        restoreOriginalName(meta, pdc);
        meta.setItemModel(null);
    }

    private void captureOriginalLore(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) return;
        List<Component> existing = meta.lore();
        List<String> serialized = existing == null
                ? List.of()
                : existing.stream().map(GSON::serialize).toList();
        pdc.set(keys.originalLore(), PersistentDataType.LIST.strings(), serialized);
    }

    private void restoreOriginalLore(ItemMeta meta, PersistentDataContainer pdc) {
        if (!pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) {
            meta.lore(null);
            return;
        }
        List<String> serialized = pdc.get(keys.originalLore(), PersistentDataType.LIST.strings());
        if (serialized == null || serialized.isEmpty()) {
            meta.lore(null);
        } else {
            meta.lore(serialized.stream().map(GSON::deserialize).toList());
        }
        pdc.remove(keys.originalLore());
    }

    private void captureOriginalName(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalName(), PersistentDataType.STRING)) return;
        if (!meta.hasDisplayName()) return;
        Component name = meta.displayName();
        if (name != null) {
            pdc.set(keys.originalName(), PersistentDataType.STRING, GSON.serialize(name));
        }
    }

    private void restoreOriginalName(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalName(), PersistentDataType.STRING)) {
            String json = pdc.get(keys.originalName(), PersistentDataType.STRING);
            meta.displayName(json != null ? GSON.deserialize(json) : null);
            pdc.remove(keys.originalName());
        } else {
            meta.displayName(null);
        }
    }

    private void captureOriginalTooltipStyle(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalTooltipStyle(), PersistentDataType.STRING)) return;
        NamespacedKey style = meta.getTooltipStyle();
        pdc.set(keys.originalTooltipStyle(), PersistentDataType.STRING,
                style != null ? style.toString() : "");
    }

    private void restoreOriginalTooltipStyle(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalTooltipStyle(), PersistentDataType.STRING)) {
            String s = pdc.get(keys.originalTooltipStyle(), PersistentDataType.STRING);
            NamespacedKey key = (s != null && !s.isEmpty()) ? NamespacedKey.fromString(s) : null;
            meta.setTooltipStyle(key);
            pdc.remove(keys.originalTooltipStyle());
        } else {
            meta.setTooltipStyle(null);
        }
    }

    // ---------- apply ----------

    private void applySkinAppearance(ItemMeta meta, PersistentDataContainer pdc, SkinDefinition skin) {
        meta.setItemModel(skin.itemModel());

        if (skin.hasDisplay()) {
            meta.displayName(MINI.deserialize(skin.display())
                    .decoration(TextDecoration.ITALIC, false));
            return;
        }
        if (pdc.has(keys.originalName(), PersistentDataType.STRING)) {
            String json = pdc.get(keys.originalName(), PersistentDataType.STRING);
            meta.displayName(json != null ? GSON.deserialize(json) : null);
        } else {
            meta.displayName(null);
        }
    }

    private void applyLore(ItemMeta meta, List<String> slots, int currentIndex) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<Component> originalLore;
        if (pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) {
            List<String> serialized = pdc.get(keys.originalLore(), PersistentDataType.LIST.strings());
            originalLore = serialized == null
                    ? Collections.emptyList()
                    : serialized.stream().map(GSON::deserialize).toList();
        } else {
            originalLore = Collections.emptyList();
        }
        meta.lore(loreRenderer.render(originalLore, slots, currentIndex));
    }
}
