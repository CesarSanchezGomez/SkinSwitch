package com.cesarcosmico.switchskin.listener;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.item.TokenFactory;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.cesarcosmico.switchskin.util.CursorUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class SkinTokenListener implements Listener {

    private final SkinSlotService skinSlotService;
    private final TokenFactory skinTokenFactory;
    private final TokenFactory tooltipTokenFactory;
    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<SwitchAnnouncer> announcerSupplier;

    public SkinTokenListener(SkinSlotService skinSlotService,
                             TokenFactory skinTokenFactory,
                             TokenFactory tooltipTokenFactory,
                             Supplier<LangConfig> langSupplier,
                             Supplier<SkinConfig> skinSupplier,
                             Supplier<SwitchAnnouncer> announcerSupplier) {
        this.skinSlotService = skinSlotService;
        this.skinTokenFactory = skinTokenFactory;
        this.tooltipTokenFactory = tooltipTokenFactory;
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.announcerSupplier = announcerSupplier;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.LEFT) return;

        final InventoryAction action = event.getAction();
        if (action != InventoryAction.SWAP_WITH_CURSOR
                && action != InventoryAction.PLACE_ALL
                && action != InventoryAction.PLACE_ONE) return;

        final ItemStack cursor = event.getCursor();
        final ItemStack target = event.getCurrentItem();
        if (target == null || target.getType().isAir()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        if (skinTokenFactory.is(cursor)) {
            if (skinTokenFactory.is(target) || tooltipTokenFactory.is(target)) return;
            event.setCancelled(true);
            handleSkinToken(player, cursor, target);
            return;
        }
        if (tooltipTokenFactory.is(cursor)) {
            if (skinTokenFactory.is(target) || tooltipTokenFactory.is(target)) return;
            event.setCancelled(true);
            handleTooltipToken(player, cursor, target);
        }
    }

    private void handleSkinToken(Player player, ItemStack cursor, ItemStack target) {
        final String skinId = skinTokenFactory.readSkinId(cursor).orElse(null);
        if (skinId == null) return;

        final SkinSlotService.AddResult result = skinSlotService.addSlot(target, skinId, player);
        switch (result) {
            case ADDED -> {
                CursorUtil.consumeOne(player, cursor);
                announcerSupplier.get().playTokenSound(player);
                final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
                final String display = def == null ? skinId : def.nameOrId();
                langSupplier.get().send(player, "command.slot-added",
                        "{skin}", display,
                        "{count}", String.valueOf(skinSlotService.getSlots(target).size()));
            }
            case DUPLICATE -> langSupplier.get().send(player, "command.duplicate-slot");
            case FULL -> langSupplier.get().send(player, "command.slots-full",
                    "{max}", String.valueOf(skinSlotService.getSlots(target).size()));
            case UNKNOWN_SKIN -> langSupplier.get().send(player, "command.unknown-skin",
                    "{skin}", skinId);
            case NO_META -> {}
        }
    }

    private void handleTooltipToken(Player player, ItemStack cursor, ItemStack target) {
        final String skinId = tooltipTokenFactory.readSkinId(cursor).orElse(null);
        if (skinId == null) return;

        final SkinSlotService.TooltipApplyResult result = skinSlotService.applyTooltip(target, skinId);
        final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        final String display = def == null ? skinId : def.nameOrId();
        switch (result) {
            case APPLIED -> {
                CursorUtil.consumeOne(player, cursor);
                announcerSupplier.get().playTokenSound(player);
                langSupplier.get().send(player, "command.tooltip-applied", "{skin}", display);
            }
            case NO_SKIN_SLOT -> langSupplier.get().send(player, "command.tooltip-needs-slot",
                    "{skin}", display);
            case ALREADY_APPLIED -> langSupplier.get().send(player, "command.tooltip-duplicate",
                    "{skin}", display);
            case NO_TOOLTIP -> langSupplier.get().send(player, "command.tooltip-missing",
                    "{skin}", skinId);
            case UNKNOWN_SKIN -> langSupplier.get().send(player, "command.unknown-skin",
                    "{skin}", skinId);
            case NO_META -> {}
        }
    }
}
