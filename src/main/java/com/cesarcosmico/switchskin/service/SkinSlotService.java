package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.item.LoreRenderer;
import com.cesarcosmico.switchskin.item.SkinSlotKeys;
import com.cesarcosmico.switchskin.placeholder.PlaceholderResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class SkinSlotService {

    public enum AddResult { ADDED, UNKNOWN_SKIN, FULL, DUPLICATE, NO_META }
    public enum RemoveResult { REMOVED, INVALID_INDEX, NO_SLOTS, NO_META }
    public enum CycleResult { CYCLED, NO_SLOTS, NO_META, SINGLE_SLOT }
    public enum SelectResult { SELECTED, NO_SLOTS, NO_META, INVALID_INDEX, ALREADY_ACTIVE }
    public enum TooltipApplyResult { APPLIED, UNKNOWN_SKIN, NO_TOOLTIP, NO_SKIN_SLOT, ALREADY_APPLIED, NO_META }
    public enum TooltipRemoveResult { REMOVED, NO_SLOTS, NOT_APPLIED, NO_META }

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SkinSlotKeys keys;
    private final LoreRenderer loreRenderer;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<PluginConfig> pluginSupplier;
    private final Supplier<PlaceholderResolver> placeholderSupplier;

    public SkinSlotService(SkinSlotKeys keys, LoreRenderer loreRenderer,
                           Supplier<SkinConfig> skinSupplier,
                           Supplier<PluginConfig> pluginSupplier,
                           Supplier<PlaceholderResolver> placeholderSupplier) {
        this.keys = keys;
        this.loreRenderer = loreRenderer;
        this.skinSupplier = skinSupplier;
        this.pluginSupplier = pluginSupplier;
        this.placeholderSupplier = placeholderSupplier;
    }

    public boolean hasSlots(ItemStack item) {
        if (item == null) return false;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keys.slots(), PersistentDataType.LIST.strings());
    }

    public List<String> getSlots(ItemStack item) {
        if (item == null) return List.of();
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return List.of();
        final List<String> data = meta.getPersistentDataContainer()
                .get(keys.slots(), PersistentDataType.LIST.strings());
        return data == null ? List.of() : data;
    }

    public int getCurrentIndex(ItemStack item) {
        if (item == null) return 0;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        final Integer idx = meta.getPersistentDataContainer().get(keys.currentIndex(), PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    public Optional<SkinDefinition> getActiveSkin(ItemStack item) {
        final List<String> slots = getSlots(item);
        if (slots.isEmpty()) return Optional.empty();
        final int idx = getCurrentIndex(item);
        if (idx < 0 || idx >= slots.size()) return Optional.empty();
        return skinSupplier.get().get(slots.get(idx));
    }

    public AddResult addSlot(ItemStack item, String skinId, @Nullable Player firstOwner) {
        if (item == null) return AddResult.NO_META;
        final Optional<SkinDefinition> skinOpt = skinSupplier.get().get(skinId);
        if (skinOpt.isEmpty()) return AddResult.UNKNOWN_SKIN;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return AddResult.NO_META;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final List<String> current = readList(pdc, keys.slots());

        if (current.size() >= pluginSupplier.get().getDefaultMaxSlots()) return AddResult.FULL;
        if (current.contains(skinId)) return AddResult.DUPLICATE;

        final boolean firstSlot = current.isEmpty();
        if (firstSlot) {
            captureOriginalAppearance(meta, pdc);
            captureOwner(pdc, firstOwner);
        }

        final SkinDefinition skin = skinOpt.get();
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
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return RemoveResult.NO_META;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) return RemoveResult.NO_SLOTS;

        final List<String> current = readList(pdc, keys.slots());
        if (index < 0 || index >= current.size()) return RemoveResult.INVALID_INDEX;

        final String removedId = current.remove(index);
        final List<String> tooltips = readList(pdc, keys.tooltipSlots());
        tooltips.remove(removedId);

        int currentIndex = readIndex(pdc);

        if (current.isEmpty()) {
            restoreOriginalAppearance(meta, pdc);
            pdc.remove(keys.slots());
            pdc.remove(keys.currentIndex());
            pdc.remove(keys.tooltipSlots());
            pdc.remove(keys.ownerUuid());
        } else {
            if (currentIndex >= current.size()) currentIndex = 0;
            pdc.set(keys.slots(), PersistentDataType.LIST.strings(), current);
            pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, currentIndex);
            writeTooltipSlots(pdc, tooltips);

            final String activeId = current.get(currentIndex);
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
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return CycleResult.NO_META;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) return CycleResult.NO_SLOTS;

        final List<String> current = readList(pdc, keys.slots());
        if (current.isEmpty()) return CycleResult.NO_SLOTS;
        if (current.size() == 1) return CycleResult.SINGLE_SLOT;

        final int next = (readIndex(pdc) + 1) % current.size();
        switchTo(meta, pdc, current, next);
        item.setItemMeta(meta);
        return CycleResult.CYCLED;
    }

    public SelectResult selectIndex(ItemStack item, int index) {
        if (item == null) return SelectResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return SelectResult.NO_META;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(keys.slots(), PersistentDataType.LIST.strings())) return SelectResult.NO_SLOTS;

        final List<String> current = readList(pdc, keys.slots());
        if (current.isEmpty()) return SelectResult.NO_SLOTS;
        if (index < 0 || index >= current.size()) return SelectResult.INVALID_INDEX;
        if (readIndex(pdc) == index) return SelectResult.ALREADY_ACTIVE;

        switchTo(meta, pdc, current, index);
        item.setItemMeta(meta);
        return SelectResult.SELECTED;
    }

    public TooltipApplyResult applyTooltip(ItemStack item, String skinId) {
        if (item == null) return TooltipApplyResult.NO_META;
        final Optional<SkinDefinition> skinOpt = skinSupplier.get().get(skinId);
        if (skinOpt.isEmpty()) return TooltipApplyResult.UNKNOWN_SKIN;
        final SkinDefinition skin = skinOpt.get();
        if (skin.tooltipStyle() == null) return TooltipApplyResult.NO_TOOLTIP;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipApplyResult.NO_META;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final List<String> slots = readList(pdc, keys.slots());
        if (!slots.contains(skinId)) return TooltipApplyResult.NO_SKIN_SLOT;

        final List<String> tooltips = readList(pdc, keys.tooltipSlots());
        if (tooltips.contains(skinId)) return TooltipApplyResult.ALREADY_APPLIED;

        tooltips.add(skinId);
        writeTooltipSlots(pdc, tooltips);

        final int idx = readIndex(pdc);
        final String activeId = (idx >= 0 && idx < slots.size()) ? slots.get(idx) : null;
        applyTooltipForActive(meta, pdc, activeId, tooltips);
        applyLore(meta, pdc, slots, idx);
        item.setItemMeta(meta);
        return TooltipApplyResult.APPLIED;
    }

    public TooltipRemoveResult removeTooltip(ItemStack item) {
        if (item == null) return TooltipRemoveResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipRemoveResult.NO_META;

        final PersistentDataContainer pdc = meta.getPersistentDataContainer();
        final List<String> slots = readList(pdc, keys.slots());
        if (slots.isEmpty()) return TooltipRemoveResult.NO_SLOTS;

        final int idx = readIndex(pdc);
        if (idx < 0 || idx >= slots.size()) return TooltipRemoveResult.NOT_APPLIED;
        final String activeId = slots.get(idx);

        final List<String> tooltips = readList(pdc, keys.tooltipSlots());
        if (!tooltips.remove(activeId)) return TooltipRemoveResult.NOT_APPLIED;

        writeTooltipSlots(pdc, tooltips);
        applyTooltipForActive(meta, pdc, activeId, tooltips);
        applyLore(meta, pdc, slots, idx);
        item.setItemMeta(meta);
        return TooltipRemoveResult.REMOVED;
    }

    private void switchTo(ItemMeta meta, PersistentDataContainer pdc,
                          List<String> slots, int index) {
        pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, index);
        final String activeId = slots.get(index);
        skinSupplier.get().get(activeId).ifPresent(s -> applySkinAppearance(meta, pdc, s));
        applyTooltipForActive(meta, pdc, activeId, readList(pdc, keys.tooltipSlots()));
        applyLore(meta, pdc, slots, index);
    }

    private List<String> readList(PersistentDataContainer pdc, NamespacedKey key) {
        if (!pdc.has(key, PersistentDataType.LIST.strings())) {
            return new ArrayList<>();
        }
        final List<String> stored = pdc.get(key, PersistentDataType.LIST.strings());
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
        final Integer idx = pdc.get(keys.currentIndex(), PersistentDataType.INTEGER);
        return idx == null ? 0 : idx;
    }

    @Nullable
    private OfflinePlayer readOwner(PersistentDataContainer pdc) {
        if (!pdc.has(keys.ownerUuid(), PersistentDataType.STRING)) return null;
        final String raw = pdc.get(keys.ownerUuid(), PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) return null;
        try {
            return Bukkit.getOfflinePlayer(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void captureOwner(PersistentDataContainer pdc, @Nullable Player firstOwner) {
        if (firstOwner == null) return;
        if (pdc.has(keys.ownerUuid(), PersistentDataType.STRING)) return;
        pdc.set(keys.ownerUuid(), PersistentDataType.STRING, firstOwner.getUniqueId().toString());
    }

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
        final List<Component> existing = meta.lore();
        final List<String> serialized = existing == null
                ? List.of()
                : existing.stream().map(GSON::serialize).toList();
        pdc.set(keys.originalLore(), PersistentDataType.LIST.strings(), serialized);
    }

    private void restoreOriginalLore(ItemMeta meta, PersistentDataContainer pdc) {
        if (!pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) {
            meta.lore(null);
            return;
        }
        final List<String> serialized = pdc.get(keys.originalLore(), PersistentDataType.LIST.strings());
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
        final Component name = meta.displayName();
        if (name != null) {
            pdc.set(keys.originalName(), PersistentDataType.STRING, GSON.serialize(name));
        }
    }

    private void restoreOriginalName(ItemMeta meta, PersistentDataContainer pdc) {
        applyOriginalName(meta, pdc);
        pdc.remove(keys.originalName());
    }

    private void applyOriginalName(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalName(), PersistentDataType.STRING)) {
            final String json = pdc.get(keys.originalName(), PersistentDataType.STRING);
            meta.displayName(json != null ? GSON.deserialize(json) : null);
        } else {
            meta.displayName(null);
        }
    }

    private void captureOriginalTooltipStyle(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalTooltipStyle(), PersistentDataType.STRING)) return;
        final NamespacedKey style = meta.getTooltipStyle();
        pdc.set(keys.originalTooltipStyle(), PersistentDataType.STRING,
                style != null ? style.toString() : "");
    }

    private void restoreOriginalTooltipStyle(ItemMeta meta, PersistentDataContainer pdc) {
        applyOriginalTooltipStyle(meta, pdc);
        pdc.remove(keys.originalTooltipStyle());
    }

    private void applyOriginalTooltipStyle(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalTooltipStyle(), PersistentDataType.STRING)) {
            final String s = pdc.get(keys.originalTooltipStyle(), PersistentDataType.STRING);
            meta.setTooltipStyle((s != null && !s.isEmpty()) ? NamespacedKey.fromString(s) : null);
        } else {
            meta.setTooltipStyle(null);
        }
    }

    private void applySkinAppearance(ItemMeta meta, PersistentDataContainer pdc, SkinDefinition skin) {
        meta.setItemModel(skin.itemModel());

        if (pluginSupplier.get().getFeatures().switchName() && skin.hasName()) {
            final String resolved = placeholderSupplier.get().resolve(readOwner(pdc), skin.name());
            meta.displayName(MINI.deserialize(resolved)
                    .decoration(TextDecoration.ITALIC, false));
            return;
        }
        applyOriginalName(meta, pdc);
    }

    private void applyTooltipForActive(ItemMeta meta, PersistentDataContainer pdc,
                                       String activeSkinId, List<String> tooltipSlots) {
        if (activeSkinId != null && tooltipSlots.contains(activeSkinId)) {
            final SkinDefinition skin = skinSupplier.get().get(activeSkinId).orElse(null);
            if (skin != null && skin.tooltipStyle() != null) {
                meta.setTooltipStyle(skin.tooltipStyle());
                return;
            }
        }
        applyOriginalTooltipStyle(meta, pdc);
    }

    private void applyLore(ItemMeta meta, PersistentDataContainer pdc,
                           List<String> slots, int currentIndex) {
        final OfflinePlayer owner = readOwner(pdc);
        final List<Component> baseLore = resolveBaseLore(pdc, slots, currentIndex, owner);
        final List<String> tooltipSlots = readList(pdc, keys.tooltipSlots());
        meta.lore(loreRenderer.render(baseLore, slots, currentIndex, tooltipSlots, owner));
    }

    private List<Component> resolveBaseLore(PersistentDataContainer pdc,
                                            List<String> slots, int currentIndex,
                                            @Nullable OfflinePlayer owner) {
        if (pluginSupplier.get().getFeatures().switchLore()
                && currentIndex >= 0 && currentIndex < slots.size()) {
            final SkinDefinition active = skinSupplier.get().get(slots.get(currentIndex)).orElse(null);
            if (active != null && active.hasLore()) {
                final PlaceholderResolver resolver = placeholderSupplier.get();
                return active.lore().stream()
                        .map(s -> resolver.resolve(owner, s))
                        .<Component>map(s -> MINI.deserialize(s).decoration(TextDecoration.ITALIC, false))
                        .toList();
            }
        }
        if (pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) {
            final List<String> serialized = pdc.get(keys.originalLore(), PersistentDataType.LIST.strings());
            return serialized == null
                    ? Collections.emptyList()
                    : serialized.stream().map(GSON::deserialize).toList();
        }
        return Collections.emptyList();
    }
}
