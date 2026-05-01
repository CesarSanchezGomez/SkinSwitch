package com.cesarcosmico.switchskin.listener;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI;
import com.cesarcosmico.switchskin.service.CooldownService;
import com.cesarcosmico.switchskin.service.PlayerLockService;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import java.util.function.Supplier;

public final class SkinMenuListener implements Listener {

    private final Supplier<LangConfig> langSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Supplier<SkinSlotService> serviceSupplier;
    private final Supplier<CooldownService> cooldownSupplier;
    private final Supplier<PlayerLockService> lockSupplier;
    private final Supplier<SwitchAnnouncer> announcerSupplier;

    public SkinMenuListener(Supplier<LangConfig> langSupplier,
                            Supplier<SkinConfig> skinSupplier,
                            Supplier<SkinSlotService> serviceSupplier,
                            Supplier<CooldownService> cooldownSupplier,
                            Supplier<PlayerLockService> lockSupplier,
                            Supplier<SwitchAnnouncer> announcerSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.serviceSupplier = serviceSupplier;
        this.cooldownSupplier = cooldownSupplier;
        this.lockSupplier = lockSupplier;
        this.announcerSupplier = announcerSupplier;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkinMenuGUI gui)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() != gui.getInventory()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        final int slot = event.getRawSlot();
        if (slot < 0 || slot >= gui.getSkinIds().size()) return;

        if (lockSupplier.get().isLocked(player)) {
            langSupplier.get().send(player, "command.switch-locked");
            player.closeInventory();
            return;
        }

        final CooldownService cooldown = cooldownSupplier.get();
        if (!cooldown.isReady(player)) {
            langSupplier.get().send(player, "command.cooldown",
                    "{seconds}", formatRemaining(cooldown.remainingMillis(player)));
            return;
        }

        final String requestedId = gui.getSkinIds().get(slot);
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        final SkinSlotService service = serviceSupplier.get();
        final int targetIndex = service.getSlots(heldItem).indexOf(requestedId);
        if (targetIndex < 0) {
            langSupplier.get().send(player, "command.skin-not-on-item", "{skin}", requestedId);
            player.closeInventory();
            return;
        }

        switch (service.selectIndex(heldItem, targetIndex)) {
            case SELECTED -> {
                player.getInventory().setItemInMainHand(heldItem);
                cooldown.mark(player);
                player.closeInventory();
                service.getActiveSkin(heldItem).ifPresent(s -> announcerSupplier.get().announceSwitch(player, s));
            }
            case ALREADY_ACTIVE -> {
                final SkinDefinition def = skinSupplier.get().get(requestedId).orElse(null);
                final String display = def == null ? requestedId : def.nameOrId();
                langSupplier.get().send(player, "command.already-active", "{skin}", display);
            }
            case NO_SLOTS, INVALID_INDEX -> langSupplier.get().send(player, "command.no-slots");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkinMenuGUI)) return;
        event.setCancelled(true);
    }

    private String formatRemaining(long millis) {
        final double seconds = millis / 1000.0;
        return String.format("%.1f", seconds);
    }
}
