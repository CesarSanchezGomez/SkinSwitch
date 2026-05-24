package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.CustomModelDataConfig;
import com.cesarcosmico.switchskin.config.TooltipDisplayConfig;
import com.cesarcosmico.switchskin.domain.id.PlayerId;
import com.cesarcosmico.switchskin.domain.id.SkinId;
import com.cesarcosmico.switchskin.domain.model.AppearanceSnapshot;
import com.cesarcosmico.switchskin.domain.model.SkinLoadout;
import com.cesarcosmico.switchskin.items.SkinSlotKeys;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import org.bukkit.Color;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The single Bukkit boundary for an item's skin state: translates the item's
 * PersistentDataContainer (and the live tooltip_display component) to and from
 * the Bukkit-free {@link SkinLoadout} / {@link AppearanceSnapshot} domain types.
 * Reuses the existing PDC keys so items skinned before the refactor stay valid.
 */
public final class SkinStateCodec {

    private static final GsonComponentSerializer GSON = GsonComponentSerializer.gson();

    private final SkinSlotKeys keys;

    public SkinStateCodec(SkinSlotKeys keys) {
        this.keys = keys;
    }

    public boolean hasLoadout(ItemMeta meta) {
        return meta != null
                && meta.getPersistentDataContainer().has(keys.slots(), PersistentDataType.LIST.strings());
    }

    public SkinLoadout readLoadout(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        List<SkinId> slots = new ArrayList<>();
        for (String raw : readStrings(pdc, keys.slots())) {
            slots.add(SkinId.of(raw));
        }
        Integer index = pdc.get(keys.currentIndex(), PersistentDataType.INTEGER);
        Set<SkinId> tooltips = new LinkedHashSet<>();
        for (String raw : readStrings(pdc, keys.tooltipSlots())) {
            tooltips.add(SkinId.of(raw));
        }
        return new SkinLoadout(slots, index == null ? 0 : index, tooltips);
    }

    public void writeLoadout(ItemMeta meta, SkinLoadout loadout) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (loadout.isEmpty()) {
            clearLoadout(meta);
            return;
        }
        pdc.set(keys.slots(), PersistentDataType.LIST.strings(), idStrings(loadout.slots()));
        pdc.set(keys.currentIndex(), PersistentDataType.INTEGER, loadout.activeIndex());
        List<String> tooltips = idStrings(new ArrayList<>(loadout.tooltips()));
        if (tooltips.isEmpty()) {
            pdc.remove(keys.tooltipSlots());
        } else {
            pdc.set(keys.tooltipSlots(), PersistentDataType.LIST.strings(), tooltips);
        }
    }

    public void clearLoadout(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(keys.slots());
        pdc.remove(keys.currentIndex());
        pdc.remove(keys.tooltipSlots());
        pdc.remove(keys.ownerUuid());
    }

    public Optional<PlayerId> readOwner(ItemMeta meta) {
        String raw = meta.getPersistentDataContainer().get(keys.ownerUuid(), PersistentDataType.STRING);
        if (raw == null || raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(PlayerId.fromString(raw));
        } catch (IllegalArgumentException malformed) {
            return Optional.empty();
        }
    }

    public void writeOwnerIfAbsent(ItemMeta meta, PlayerId owner) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(keys.ownerUuid(), PersistentDataType.STRING)) {
            return;
        }
        pdc.set(keys.ownerUuid(), PersistentDataType.STRING, owner.value().toString());
    }

    public void captureAppearance(ItemStack item, ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (!pdc.has(keys.originalItemModel(), PersistentDataType.STRING)) {
            NamespacedKey model = meta.getItemModel();
            pdc.set(keys.originalItemModel(), PersistentDataType.STRING, model != null ? model.toString() : "");
        }
        if (!pdc.has(keys.originalTooltipStyle(), PersistentDataType.STRING)) {
            NamespacedKey style = meta.getTooltipStyle();
            pdc.set(keys.originalTooltipStyle(), PersistentDataType.STRING, style != null ? style.toString() : "");
        }
        if (!pdc.has(keys.originalLore(), PersistentDataType.LIST.strings())) {
            List<Component> lore = meta.lore();
            List<String> serialized = lore == null ? List.of() : lore.stream().map(GSON::serialize).toList();
            pdc.set(keys.originalLore(), PersistentDataType.LIST.strings(), serialized);
        }
        if (!pdc.has(keys.originalName(), PersistentDataType.STRING) && meta.hasDisplayName()) {
            Component name = meta.displayName();
            if (name != null) {
                pdc.set(keys.originalName(), PersistentDataType.STRING, GSON.serialize(name));
            }
        }
        captureCmd(meta, pdc);
        captureTooltipDisplay(item, pdc);
    }

    public AppearanceSnapshot readAppearance(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean present = pdc.has(keys.originalItemModel(), PersistentDataType.STRING)
                || pdc.has(keys.originalLore(), PersistentDataType.LIST.strings());

        String itemModel = emptyToNull(pdc.get(keys.originalItemModel(), PersistentDataType.STRING));
        String tooltipStyle = emptyToNull(pdc.get(keys.originalTooltipStyle(), PersistentDataType.STRING));
        String nameJson = pdc.get(keys.originalName(), PersistentDataType.STRING);
        List<String> loreJson = readStrings(pdc, keys.originalLore());

        return new AppearanceSnapshot(present, loreJson, nameJson, itemModel, tooltipStyle,
                readCmd(pdc), readTooltipDisplay(pdc));
    }

    public void clearAppearance(ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.remove(keys.originalItemModel());
        pdc.remove(keys.originalTooltipStyle());
        pdc.remove(keys.originalLore());
        pdc.remove(keys.originalName());
        pdc.remove(keys.originalCmdPresent());
        pdc.remove(keys.originalCmdFloats());
        pdc.remove(keys.originalCmdFlags());
        pdc.remove(keys.originalCmdStrings());
        pdc.remove(keys.originalCmdColors());
        pdc.remove(keys.originalTooltipDisplayPresent());
        pdc.remove(keys.originalTooltipDisplayHide());
        pdc.remove(keys.originalTooltipDisplayHidden());
    }

    private void captureCmd(ItemMeta meta, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalCmdPresent(), PersistentDataType.BYTE)) {
            return;
        }
        CustomModelDataComponent cmd = meta.getCustomModelDataComponent();
        boolean hasAny = !cmd.getFloats().isEmpty() || !cmd.getFlags().isEmpty()
                || !cmd.getStrings().isEmpty() || !cmd.getColors().isEmpty();
        pdc.set(keys.originalCmdPresent(), PersistentDataType.BYTE, (byte) (hasAny ? 1 : 0));
        if (!hasAny) {
            return;
        }
        pdc.set(keys.originalCmdFloats(), PersistentDataType.LIST.floats(), cmd.getFloats());
        pdc.set(keys.originalCmdFlags(), PersistentDataType.LIST.booleans(), cmd.getFlags());
        pdc.set(keys.originalCmdStrings(), PersistentDataType.LIST.strings(), cmd.getStrings());
        pdc.set(keys.originalCmdColors(), PersistentDataType.LIST.integers(),
                cmd.getColors().stream().map(Color::asRGB).toList());
    }

    @Nullable
    private CustomModelDataConfig readCmd(PersistentDataContainer pdc) {
        Byte present = pdc.get(keys.originalCmdPresent(), PersistentDataType.BYTE);
        if (present == null || present == 0) {
            return null;
        }
        List<Float> floats = pdc.getOrDefault(keys.originalCmdFloats(), PersistentDataType.LIST.floats(), List.of());
        List<Boolean> flags = pdc.getOrDefault(keys.originalCmdFlags(), PersistentDataType.LIST.booleans(), List.of());
        List<String> strings = pdc.getOrDefault(keys.originalCmdStrings(), PersistentDataType.LIST.strings(), List.of());
        List<Integer> colors = pdc.getOrDefault(keys.originalCmdColors(), PersistentDataType.LIST.integers(), List.of());
        return new CustomModelDataConfig(floats, flags, strings, colors);
    }

    private void captureTooltipDisplay(ItemStack item, PersistentDataContainer pdc) {
        if (pdc.has(keys.originalTooltipDisplayPresent(), PersistentDataType.BYTE)) {
            return;
        }
        TooltipDisplay td = item.getData(DataComponentTypes.TOOLTIP_DISPLAY);
        if (td == null) {
            pdc.set(keys.originalTooltipDisplayPresent(), PersistentDataType.BYTE, (byte) 0);
            return;
        }
        pdc.set(keys.originalTooltipDisplayPresent(), PersistentDataType.BYTE, (byte) 1);
        pdc.set(keys.originalTooltipDisplayHide(), PersistentDataType.BYTE, (byte) (td.hideTooltip() ? 1 : 0));
        List<String> hidden = new ArrayList<>();
        td.hiddenComponents().forEach(type -> hidden.add(type.getKey().value()));
        pdc.set(keys.originalTooltipDisplayHidden(), PersistentDataType.LIST.strings(), hidden);
    }

    @Nullable
    private TooltipDisplayConfig readTooltipDisplay(PersistentDataContainer pdc) {
        Byte present = pdc.get(keys.originalTooltipDisplayPresent(), PersistentDataType.BYTE);
        if (present == null || present == 0) {
            return null;
        }
        boolean hide = pdc.getOrDefault(keys.originalTooltipDisplayHide(), PersistentDataType.BYTE, (byte) 0) == 1;
        List<String> hidden = pdc.getOrDefault(keys.originalTooltipDisplayHidden(),
                PersistentDataType.LIST.strings(), List.of());
        return new TooltipDisplayConfig(hide, hidden);
    }

    private static List<String> readStrings(PersistentDataContainer pdc, NamespacedKey key) {
        if (!pdc.has(key, PersistentDataType.LIST.strings())) {
            return new ArrayList<>();
        }
        List<String> stored = pdc.get(key, PersistentDataType.LIST.strings());
        return stored == null ? new ArrayList<>() : new ArrayList<>(stored);
    }

    private static List<String> idStrings(List<SkinId> ids) {
        return ids.stream().map(SkinId::value).toList();
    }

    @Nullable
    private static String emptyToNull(@Nullable String value) {
        return (value == null || value.isEmpty()) ? null : value;
    }
}
