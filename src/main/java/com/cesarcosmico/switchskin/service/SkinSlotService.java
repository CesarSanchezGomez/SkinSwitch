package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.domain.id.PlayerId;
import com.cesarcosmico.switchskin.domain.id.SkinId;
import com.cesarcosmico.switchskin.domain.model.AppearanceSnapshot;
import com.cesarcosmico.switchskin.domain.model.SkinLoadout;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

// Thin orchestrator: read loadout -> run a SkinLoadout transition -> persist -> re-render.
public final class SkinSlotService {

    public static final int VANILLA_INDEX = SkinLoadout.VANILLA_INDEX;

    public enum AddResult { ADDED, UNKNOWN_SKIN, FULL, DUPLICATE, INCOMPATIBLE, NO_META }
    public enum RemoveResult { REMOVED, INVALID_INDEX, NO_SLOTS, NO_META }
    public enum CycleResult { CYCLED, NO_SLOTS, NO_META, SINGLE_SLOT }
    public enum SelectResult { SELECTED, NO_SLOTS, NO_META, INVALID_INDEX, ALREADY_ACTIVE }
    public enum VanillaResult { APPLIED, ALREADY_VANILLA, NO_SLOTS, NO_META }
    public enum TooltipApplyResult { APPLIED, UNKNOWN_SKIN, NO_TOOLTIP, NO_SKIN_SLOT, ALREADY_APPLIED, NO_META }
    public enum TooltipRemoveResult { REMOVED, NO_SLOTS, NOT_APPLIED, NO_META }

    private final SkinStateCodec codec;
    private final SkinAppearanceRenderer renderer;
    private final SkinConfig skinConfig;
    private final PluginConfig pluginConfig;
    private final TooltipService tooltipService;

    public SkinSlotService(SkinStateCodec codec, SkinAppearanceRenderer renderer,
                           SkinConfig skinConfig, PluginConfig pluginConfig,
                           TooltipService tooltipService) {
        this.codec = codec;
        this.renderer = renderer;
        this.skinConfig = skinConfig;
        this.pluginConfig = pluginConfig;
        this.tooltipService = tooltipService;
    }

    public boolean hasSlots(ItemStack item) {
        if (item == null) return false;
        final ItemMeta meta = item.getItemMeta();
        return meta != null && codec.hasLoadout(meta);
    }

    public List<String> getSlots(ItemStack item) {
        if (item == null) return List.of();
        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !codec.hasLoadout(meta)) return List.of();
        return codec.readLoadout(meta).slots().stream().map(SkinId::value).toList();
    }

    public int getCurrentIndex(ItemStack item) {
        if (item == null) return 0;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !codec.hasLoadout(meta)) return 0;
        return codec.readLoadout(meta).activeIndex();
    }

    public Optional<SkinDefinition> getActiveSkin(ItemStack item) {
        if (item == null) return Optional.empty();
        final ItemMeta meta = item.getItemMeta();
        if (meta == null || !codec.hasLoadout(meta)) return Optional.empty();
        return codec.readLoadout(meta).activeSkin().flatMap(id -> skinConfig.get(id.value()));
    }

    public boolean isVanillaActive(ItemStack item) {
        if (!hasSlots(item)) return false;
        final ItemMeta meta = item.getItemMeta();
        return meta != null && codec.readLoadout(meta).isVanillaActive();
    }

    public AddResult addSlot(ItemStack item, String skinId, @Nullable Player firstOwner) {
        if (item == null) return AddResult.NO_META;
        final SkinDefinition skin = skinConfig.get(skinId).orElse(null);
        if (skin == null) return AddResult.UNKNOWN_SKIN;
        if (!skin.acceptsMaterial(item.getType())) return AddResult.INCOMPATIBLE;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return AddResult.NO_META;

        final SkinLoadout loadout = codec.readLoadout(meta);
        final boolean firstSlot = loadout.isEmpty();
        final SkinLoadout.Change<SkinLoadout.AddOutcome> change =
                loadout.add(SkinId.of(skinId), pluginConfig.getDefaultMaxSlots());
        switch (change.outcome()) {
            case FULL -> { return AddResult.FULL; }
            case DUPLICATE -> { return AddResult.DUPLICATE; }
            case ADDED -> { /* fall through */ }
        }

        if (firstSlot) {
            codec.captureAppearance(item, meta);
            if (firstOwner != null) {
                codec.writeOwnerIfAbsent(meta, PlayerId.of(firstOwner.getUniqueId()));
            }
        }

        codec.writeLoadout(meta, change.loadout());
        renderer.render(item, meta, change.loadout());
        return AddResult.ADDED;
    }

    public RemoveResult removeSlot(ItemStack item, int index) {
        if (item == null) return RemoveResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return RemoveResult.NO_META;
        if (!codec.hasLoadout(meta)) return RemoveResult.NO_SLOTS;

        final SkinLoadout loadout = codec.readLoadout(meta);
        final SkinLoadout.Change<SkinLoadout.RemoveOutcome> change = loadout.remove(index);
        if (change.outcome() == SkinLoadout.RemoveOutcome.INVALID_INDEX) {
            return RemoveResult.INVALID_INDEX;
        }

        if (change.loadout().isEmpty()) {
            final AppearanceSnapshot snapshot = codec.readAppearance(meta);
            codec.clearAppearance(meta);
            codec.clearLoadout(meta);
            renderer.restoreOriginal(item, meta, snapshot);
        } else {
            codec.writeLoadout(meta, change.loadout());
            renderer.render(item, meta, change.loadout());
        }
        return RemoveResult.REMOVED;
    }

    public CycleResult cycleNext(ItemStack item) {
        if (item == null) return CycleResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return CycleResult.NO_META;
        if (!codec.hasLoadout(meta)) return CycleResult.NO_SLOTS;

        final SkinLoadout loadout = codec.readLoadout(meta);
        final SkinLoadout.Change<SkinLoadout.CycleOutcome> change = loadout.cycleNext();
        switch (change.outcome()) {
            case EMPTY -> { return CycleResult.NO_SLOTS; }
            case SINGLE_SLOT -> { return CycleResult.SINGLE_SLOT; }
            case CYCLED -> { /* fall through */ }
        }

        codec.writeLoadout(meta, change.loadout());
        renderer.render(item, meta, change.loadout());
        return CycleResult.CYCLED;
    }

    public SelectResult selectIndex(ItemStack item, int index) {
        if (item == null) return SelectResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return SelectResult.NO_META;
        if (!codec.hasLoadout(meta)) return SelectResult.NO_SLOTS;

        final SkinLoadout loadout = codec.readLoadout(meta);
        if (loadout.isEmpty()) return SelectResult.NO_SLOTS;
        final SkinLoadout.Change<SkinLoadout.SelectOutcome> change = loadout.select(index);
        switch (change.outcome()) {
            case EMPTY -> { return SelectResult.NO_SLOTS; }
            case INVALID_INDEX -> { return SelectResult.INVALID_INDEX; }
            case ALREADY_ACTIVE -> { return SelectResult.ALREADY_ACTIVE; }
            case SELECTED -> { /* fall through */ }
        }

        codec.writeLoadout(meta, change.loadout());
        renderer.render(item, meta, change.loadout());
        return SelectResult.SELECTED;
    }

    public VanillaResult selectVanilla(ItemStack item) {
        if (item == null) return VanillaResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return VanillaResult.NO_META;
        if (!codec.hasLoadout(meta)) return VanillaResult.NO_SLOTS;

        final SkinLoadout loadout = codec.readLoadout(meta);
        final SkinLoadout.Change<SkinLoadout.VanillaOutcome> change = loadout.selectVanilla();
        switch (change.outcome()) {
            case EMPTY -> { return VanillaResult.NO_SLOTS; }
            case ALREADY_VANILLA -> { return VanillaResult.ALREADY_VANILLA; }
            case APPLIED -> { /* fall through */ }
        }

        codec.writeLoadout(meta, change.loadout());
        renderer.render(item, meta, change.loadout());
        return VanillaResult.APPLIED;
    }

    public TooltipApplyResult applyTooltip(ItemStack item, String skinId) {
        return tooltipService.applyTooltip(item, skinId);
    }

    public TooltipRemoveResult removeTooltip(ItemStack item) {
        return tooltipService.removeTooltip(item);
    }
}
