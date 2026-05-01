package com.cesarcosmico.skinswitch.listener;

import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.item.SkinTokenFactory;
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
    private final SkinTokenFactory tokenFactory;
    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;

    public SkinSlotListener(SkinSlotService skinSlotService,
                            SkinTokenFactory tokenFactory,
                            Supplier<LangConfig> langSupplier,
                            Supplier<SkinConfig> skinSupplier) {
        this.skinSlotService = skinSlotService;
        this.tokenFactory = tokenFactory;
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getPlayer().isSneaking()) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR
                && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        ItemStack item = event.getItem();
        if (!skinSlotService.hasSlots(item)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("skinswitch.use")) {
            langSupplier.get().send(player, "command.no-permission");
            return;
        }

        SkinSlotService.CycleResult result = skinSlotService.cycleNext(item);
        if (result == SkinSlotService.CycleResult.CYCLED) {
            event.setCancelled(true);
            skinSlotService.getActiveSkin(item).ifPresent(s ->
                    langSupplier.get().send(player, "skin.switched", "{skin}", s.display()));
        } else if (result == SkinSlotService.CycleResult.SINGLE_SLOT) {
            event.setCancelled(true);
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
        if (!tokenFactory.isToken(cursor)) return;
        if (target == null || target.getType().isAir()) return;
        if (tokenFactory.isToken(target)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        String skinId = tokenFactory.readSkinId(cursor).orElse(null);
        if (skinId == null) return;

        event.setCancelled(true);

        SkinSlotService.AddResult result = skinSlotService.addSlot(target, skinId);
        switch (result) {
            case ADDED -> {
                consumeOne(player, cursor);
                SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
                String display = def == null ? skinId : def.display();
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
