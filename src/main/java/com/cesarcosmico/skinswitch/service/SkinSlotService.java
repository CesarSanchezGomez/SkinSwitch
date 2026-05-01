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
 * Skin slots own item_model + custom_name + lore. Tooltip styles are
 * tracked per-slot via the tooltip_slots list; the active slot's tooltip
 * (if any) is what's actually applied to the item, otherwise the item's
 * captured original tooltip is restored.
 */
public final class SkinSlotService {

    public enum AddResult { ADDED, UNKNOWN_SKIN, FULL, DUPLICATE, NO_META }
    public enum RemoveResult { REMOVED, INVALID_INDEX, NO_SLOTS, NO_META }
    public enum CycleResult { CYCLED, NO_SLOTS, NO_META, SINGLE_SLOT }
    public enum TooltipApplyResult { APPLIED, UNKNOWN_SKIN, NO_TOOLTIP, NO_SKIN_SLOT, ALREADY_APPLIED, NO_META }
    public enum TooltipRemoveResult { REMOVED, NO_SLOTS, NOT_APPLIED, NO_META }

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

    public AddResult addSlot(ItemStack item, String skinId) {
        if (item == null) return AddResult.NO_META;
        Optional<SkinDefinition> skinOpt = skinSupplier.get().get(skinId);
        if (skinOpt.isEmpty()) return AddResult.UNKNOWN_SKIN;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return AddResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<String> current = readList(pdc, keys.slots());

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

        applyLore(meta, pdc, current, currentIndex);
        item.setItemMeta(meta);
        return AddResult.ADDED;
    }

    public RemoveResult removeSlot(ItemStack item, int index) {
        if (item == null) return RemoveResult.NO_META;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return RemoveResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) return RemoveResult.NO_SLOTS;

        List<String> current = readList(pdc, keys.slots());
        if (index < 0 || index >= current.size()) return RemoveResult.INVALID_INDEX;

        String removedId = current.remove(index);
        List<String> tooltips = readList(pdc, keys.tooltipSlots());
        tooltips.remove(removedId);

        int currentIndex = readIndex(pdc);

        if (current.isEmpty()) {
            restoreOriginalAppearance(meta, pdc);
            pdc.remove(keys.slots());
            pdc.remove(keys.currentIndex());
            pdc.remove(keys.tooltipSlots());
        } else {
            if (currentIndex >= current.size()) currentIndex = 0;
            pdc.set(keys.slots(), PersistentDataType.LIST.strings(), current);
            pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, currentIndex);
            writeTooltipSlots(pdc, tooltips);

            String activeId = current.get(currentIndex);
            skinSupplier.get().get(activeId)
                    .ifPresent(s -> applySkinAppearance(meta, pdc, s));
            applyTooltipForActive(meta, pdc, activeId, tooltips);
            applyLore(meta, pdc, current, currentIndex);
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

        List<String> current = readList(pdc, keys.slots());
        if (current.isEmpty()) return CycleResult.NO_SLOTS;
        if (current.size() == 1) return CycleResult.SINGLE_SLOT;

        int next = (readIndex(pdc) + 1) % current.size();
        pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, next);

        String activeId = current.get(next);
        skinSupplier.get().get(activeId)
                .ifPresent(s -> applySkinAppearance(meta, pdc, s));
        applyTooltipForActive(meta, pdc, activeId, readList(pdc, keys.tooltipSlots()));
        applyLore(meta, pdc, current, next);
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
        List<String> slots = readList(pdc, keys.slots());
        if (!slots.contains(skinId)) return TooltipApplyResult.NO_SKIN_SLOT;

        List<String> tooltips = readList(pdc, keys.tooltipSlots());
        if (tooltips.contains(skinId)) return TooltipApplyResult.ALREADY_APPLIED;

        tooltips.add(skinId);
        writeTooltipSlots(pdc, tooltips);

        int idx = readIndex(pdc);
        String activeId = (idx >= 0 && idx < slots.size()) ? slots.get(idx) : null;
        applyTooltipForActive(meta, pdc, activeId, tooltips);
        applyLore(meta, pdc, slots, idx);
        item.setItemMeta(meta);
        return TooltipApplyResult.APPLIED;
    }

    public TooltipRemoveResult removeTooltip(ItemStack item) {
        if (item == null) return TooltipRemoveResult.NO_META;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipRemoveResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<String> slots = readList(pdc, keys.slots());
        if (slots.isEmpty()) return TooltipRemoveResult.NO_SLOTS;

        int idx = readIndex(pdc);
        if (idx < 0 || idx >= slots.size()) return TooltipRemoveResult.NOT_APPLIED;
        String activeId = slots.get(idx);

        List<String> tooltips = readList(pdc, keys.tooltipSlots());
        if (!tooltips.remove(activeId)) return TooltipRemoveResult.NOT_APPLIED;

        writeTooltipSlots(pdc, tooltips);
        applyTooltipForActive(meta, pdc, activeId, tooltips);
        applyLore(meta, pdc, slots, idx);
        item.setItemMeta(meta);
        return TooltipRemoveResult.REMOVED;
    }

    private List<String> readList(PersistentDataContainer pdc, NamespacedKey key) {
        if (!pdc.has(key, PersistentDataType.LIST.strings())) {
            return new ArrayList<>();
        }
        List<String> stored = pdc.get(key, PersistentDataType.LIST.strings());
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    private void writeTooltipSlots(PersistentDataContainer pdc, List<String> tooltips) {
        if (tooltips.isEmpty()) {
            pdc.remove(keys.tooltipSlots());
        } else {
            pdc.set(keys.tooltipSlots(), PersistentDataType.LIST.strings(), tooltips);
        }
    }

    private int readIndex(PersistentDataContainer pdc) {
        Integer idx = pdc.get(keys.currentIndex(), PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    // ---------- capture / restore ----------

    private void captureOriginalAppearance(ItemMeta meta, PersistentDataContainer pdc) {
        captureOriginalLore(meta, pdc);
        captureOriginalName(meta, pdc);
        captureOriginalTooltipStyle(meta, pdc);
    }

    private void restoreOriginalAppearance(ItemMeta meta, PersistentDataContainer pdc) {
        restoreOriginalLore(meta, pdc);
        restoreOriginalName(meta, pdc);
        restoreOriginalTooltipStyle(meta, pdc);
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
            meta.setTooltipStyle((s != null && !s.isEmpty()) ? NamespacedKey.fromString(s) : null);
            pdc.remove(keys.originalTooltipStyle());
        } else {
            meta.setTooltipStyle(null);
        }
    }

    // ---------- apply ----------

    private void applySkinAppearance(ItemMeta meta, PersistentDataContainer pdc, SkinDefinition skin) {
        meta.setItemModel(skin.itemModel());

        if (pluginSupplier.get().getFeatures().switchName() && skin.hasDisplay()) {
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

    private void applyTooltipForActive(ItemMeta meta, PersistentDataContainer pdc,
                                       String activeSkinId, List<String> tooltipSlots) {
        if (activeSkinId != null && tooltipSlots.contains(activeSkinId)) {
            SkinDefinition skin = skinSupplier.get().get(activeSkinId).orElse(null);
            if (skin != null && skin.tooltipStyle() != null) {
                meta.setTooltipStyle(skin.tooltipStyle());
                return;
            }
        }
        if (pdc.has(keys.originalTooltipStyle(), PersistentDataType.STRING)) {
            String s = pdc.get(keys.originalTooltipStyle(), PersistentDataType.STRING);
            meta.setTooltipStyle((s != null && !s.isEmpty()) ? NamespacedKey.fromString(s) : null);
        } else {
            meta.setTooltipStyle(null);
        }
    }

    private void applyLore(ItemMeta meta, PersistentDataContainer pdc,
                           List<String> slots, int currentIndex) {
        List<Component> baseLore = resolveBaseLore(pdc, slots, currentIndex);
        List<String> tooltipSlots = readList(pdc, keys.tooltipSlots());
        meta.lore(loreRenderer.render(baseLore, slots, currentIndex, tooltipSlots));
    }

    private List<Component> resolveBaseLore(PersistentDataContainer pdc,
                                            List<String> slots, int currentIndex) {
        if (pluginSupplier.get().getFeatures().switchLore()
                && currentIndex >= 0 && currentIndex < slots.size()) {
            SkinDefinition active = skinSupplier.get().get(slots.get(currentIndex)).orElse(null);
            if (active != null && active.hasLore()) {
                return active.lore().stream()
                        .<Component>map(s -> MINI.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
            }
        }
        if (pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) {
            List<String> serialized = pdc.get(keys.originalLore(), PersistentDataType.LIST.strings());
            return serialized == null
                    ? Collections.emptyList()
                    : serialized.stream().map(GSON::deserialize).toList();
        }
        return Collections.emptyList();
    }
}
