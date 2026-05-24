package com.cesarcosmico.switchskin.service;

import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.domain.id.SkinId;
import com.cesarcosmico.switchskin.domain.model.SkinLoadout;
import com.cesarcosmico.switchskin.service.SkinSlotService.TooltipApplyResult;
import com.cesarcosmico.switchskin.service.SkinSlotService.TooltipRemoveResult;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Use-cases for the tooltip token / removal flow. Reads and writes state through
 * {@link SkinStateCodec}, runs the pure {@link SkinLoadout} tooltip transitions
 * and re-renders via {@link SkinAppearanceRenderer}.
 */
public final class TooltipService {

    private final SkinStateCodec codec;
    private final SkinAppearanceRenderer renderer;
    private final SkinConfig skinConfig;

    public TooltipService(SkinStateCodec codec, SkinAppearanceRenderer renderer, SkinConfig skinConfig) {
        this.codec = codec;
        this.renderer = renderer;
        this.skinConfig = skinConfig;
    }

    public TooltipApplyResult applyTooltip(ItemStack item, String skinId) {
        if (item == null) return TooltipApplyResult.NO_META;
        final SkinDefinition skin = skinConfig.get(skinId).orElse(null);
        if (skin == null) return TooltipApplyResult.UNKNOWN_SKIN;
        if (skin.tooltipStyle() == null) return TooltipApplyResult.NO_TOOLTIP;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipApplyResult.NO_META;

        final SkinLoadout loadout = codec.readLoadout(meta);
        final SkinLoadout.Change<SkinLoadout.TooltipAddOutcome> change = loadout.addTooltip(SkinId.of(skinId));
        switch (change.outcome()) {
            case NO_SLOT -> { return TooltipApplyResult.NO_SKIN_SLOT; }
            case ALREADY -> { return TooltipApplyResult.ALREADY_APPLIED; }
            case ADDED -> { /* fall through */ }
        }

        codec.writeLoadout(meta, change.loadout());
        renderer.render(item, meta, change.loadout());
        return TooltipApplyResult.APPLIED;
    }

    public TooltipRemoveResult removeTooltip(ItemStack item) {
        if (item == null) return TooltipRemoveResult.NO_META;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return TooltipRemoveResult.NO_META;

        final SkinLoadout loadout = codec.readLoadout(meta);
        if (loadout.isEmpty()) return TooltipRemoveResult.NO_SLOTS;

        final SkinId active = loadout.activeSkin().orElse(null);
        if (active == null) return TooltipRemoveResult.NOT_APPLIED;

        final SkinLoadout.Change<SkinLoadout.TooltipRemoveOutcome> change = loadout.removeTooltip(active);
        if (change.outcome() == SkinLoadout.TooltipRemoveOutcome.NONE) {
            return TooltipRemoveResult.NOT_APPLIED;
        }

        codec.writeLoadout(meta, change.loadout());
        renderer.render(item, meta, change.loadout());
        return TooltipRemoveResult.REMOVED;
    }
}
