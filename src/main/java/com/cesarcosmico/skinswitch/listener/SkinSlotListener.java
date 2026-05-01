package com.cesarcosmico.skinswitch.listener;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.item.TokenFactory;
import com.cesarcosmico.skinswitch.service.SkinSlotService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class SkinSlotListener implements Listener {

    private final SkinSlotService skinSlotService;
    private final TokenFactory skinTokenFactory;
    private final TokenFactory tooltipTokenFactory;
    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;

    public SkinSlotListener(SkinSlotService skinSlotService,
                            TokenFactory skinTokenFactory,
                            TokenFactory tooltipTokenFactory,
                            Supplier<LangConfig> langSupplier,
                            Supplier<SkinConfig> skinSupplier) {
        this.skinSlotService = skinSlotService;
        this.skinTokenFactory = skinTokenFactory;
        this.tooltipTokenFactory = tooltipTokenFactory;
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
    }

    // Not using ignoreCancelled: vanilla pre-cancels sneak+right-click on
    // many blocks (chests, doors, etc.) which would otherwise hide the event.
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;

        Player player = event.getPlayer();
        if (!player.isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        if (!skinSlotService.hasSlots(item)) return;

        if (!player.hasPermission("skinswitch.use")) {
            langSupplier.get().send(player, "command.no-permission");
            return;
        }

        SkinSlotService.CycleResult result = skinSlotService.cycleNext(item);
        if (result == SkinSlotService.CycleResult.CYCLED) {
            player.getInventory().setItemInMainHand(item);
            event.setCancelled(true);
            skinSlotService.getActiveSkin(item).ifPresent(s ->
                    langSupplier.get().send(player, "skin.switched", "{skin}", s.nameOrId()));
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (event.getClick() != ClickType.RIGHT && event.getClick() != ClickType.LEFT) return;

        InventoryAction action = event.getAction();
        if (action != InventoryAction.SWAP_WITH_CURSOR
                && action != InventoryAction.PLACE_ALL
                && action != InventoryAction.PLACE_ONE) return;

        ItemStack cursor = event.getCursor();
        ItemStack target = event.getCurrentItem();
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
        String skinId = skinTokenFactory.readSkinId(cursor).orElse(null);
        if (skinId == null) return;

        SkinSlotService.AddResult result = skinSlotService.addSlot(target, skinId, player);
        switch (result) {
            case ADDED -> {
                consumeOne(player, cursor);
                SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
                String display = def == null ? skinId : def.nameOrId();
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
        String skinId = tooltipTokenFactory.readSkinId(cursor).orElse(null);
        if (skinId == null) return;

        SkinSlotService.TooltipApplyResult result = skinSlotService.applyTooltip(target, skinId);
        SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        String display = def == null ? skinId : def.nameOrId();
        switch (result) {
            case APPLIED -> {
                consumeOne(player, cursor);
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

    private void consumeOne(Player player, ItemStack cursor) {
        int newAmount = cursor.getAmount() - 1;
        if (newAmount <= 0) {
            player.setItemOnCursor(null);
        } else {
            ItemStack reduced = cursor.clone();
            reduced.setAmount(newAmount);
            player.setItemOnCursor(reduced);
        }
    }
}
