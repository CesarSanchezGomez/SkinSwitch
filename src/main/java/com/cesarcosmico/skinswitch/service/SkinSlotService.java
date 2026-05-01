package com.cesarcosmico.skinswitch.service;

import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.item.LoreRenderer;
import com.cesarcosmico.skinswitch.item.SkinSlotKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
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
 * Only the item_model component and lore are touched; durability,
 * enchantments, attribute modifiers and any other PDC are preserved.
 * The item's pre-existing lore is captured into PDC the first time a
 * slot is added and restored verbatim when the last slot is removed.
 */
public final class SkinSlotService {

    public enum AddResult { ADDED, UNKNOWN_SKIN, FULL, DUPLICATE, NO_META }
    public enum RemoveResult { REMOVED, INVALID_INDEX, NO_SLOTS, NO_META }
    public enum CycleResult { CYCLED, NO_SLOTS, NO_META, SINGLE_SLOT }

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

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
        Optional<SkinDefinition> skin = skinSupplier.get().get(skinId);
        if (skin.isEmpty()) return AddResult.UNKNOWN_SKIN;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return AddResult.NO_META;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<String> current = readSlots(pdc);

        if (current.size() >= pluginSupplier.get().getDefaultMaxSlots()) return AddResult.FULL;
        if (current.contains(skinId)) return AddResult.DUPLICATE;

        boolean firstSlot = current.isEmpty();
        if (firstSlot) {
            captureOriginalLore(meta, pdc);
        }

        current.add(skinId);
        pdc.set(keys.slots(), PersistentDataType.LIST.strings(), current);

        int currentIndex = readIndex(pdc);
        if (firstSlot) {
            currentIndex = 0;
            pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, 0);
            applyItemModel(meta, skin.get());
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
            restoreOriginalLore(meta, pdc);
            meta.setItemModel(null);
            pdc.remove(keys.slots());
            pdc.remove(keys.currentIndex());
        } else {
            if (currentIndex >= current.size()) currentIndex = 0;
            pdc.set(keys.slots(), PersistentDataType.LIST.strings(), current);
            pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, currentIndex);
            int finalIndex = currentIndex;
            skinSupplier.get().get(current.get(finalIndex)).ifPresent(s -> applyItemModel(meta, s));
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
        skinSupplier.get().get(current.get(next)).ifPresent(s -> applyItemModel(meta, s));
        applyLore(meta, current, next);
        item.setItemMeta(meta);
        return CycleResult.CYCLED;
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

    private void applyItemModel(ItemMeta meta, SkinDefinition skin) {
        meta.setItemModel(skin.itemModel());
    }
}
