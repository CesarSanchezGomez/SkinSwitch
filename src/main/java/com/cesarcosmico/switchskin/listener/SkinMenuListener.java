package com.cesarcosmico.switchskin.listener;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI.MenuAction;
import com.cesarcosmico.switchskin.service.CooldownService;
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
    private final Supplier<SwitchAnnouncer> announcerSupplier;

    public SkinMenuListener(Supplier<LangConfig> langSupplier,
                            Supplier<SkinConfig> skinSupplier,
                            Supplier<SkinSlotService> serviceSupplier,
                            Supplier<CooldownService> cooldownSupplier,
                            Supplier<SwitchAnnouncer> announcerSupplier) {
        this.langSupplier = langSupplier;
        this.skinSupplier = skinSupplier;
        this.serviceSupplier = serviceSupplier;
        this.cooldownSupplier = cooldownSupplier;
        this.announcerSupplier = announcerSupplier;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkinMenuGUI gui)) return;
        event.setCancelled(true);

        if (event.getClickedInventory() != gui.getInventory()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        final MenuAction action = gui.actionAt(event.getRawSlot());
        if (action == null) return;

        switch (action) {
            case MenuAction.Close ignored -> player.closeInventory();
            case MenuAction.SelectSkin select -> handleSkinSelect(player, select.skinId());
            case MenuAction.SelectVanilla ignored -> handleVanilla(player);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkinMenuGUI)) return;
        event.setCancelled(true);
    }

    private void handleSkinSelect(Player player, String skinId) {
        if (notReady(player)) return;

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        final SkinSlotService service = serviceSupplier.get();
        final int targetIndex = service.getSlots(heldItem).indexOf(skinId);
        if (targetIndex < 0) {
            langSupplier.get().send(player, "command.skin-not-on-item", "{skin}", skinId);
            player.closeInventory();
            return;
        }

        switch (service.selectIndex(heldItem, targetIndex)) {
            case SELECTED -> {
                player.getInventory().setItemInMainHand(heldItem);
                cooldownSupplier.get().mark(player);
                player.closeInventory();
                service.getActiveSkin(heldItem).ifPresent(s -> announcerSupplier.get().announceSwitch(player, s));
            }
            case ALREADY_ACTIVE -> {
                final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
                final String display = def == null ? skinId : def.nameOrId();
                langSupplier.get().send(player, "command.already-active", "{skin}", display);
            }
            case NO_SLOTS, INVALID_INDEX -> langSupplier.get().send(player, "command.no-slots");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
    }

    private void handleVanilla(Player player) {
        if (notReady(player)) return;

        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        final SkinSlotService service = serviceSupplier.get();

        switch (service.selectVanilla(heldItem)) {
            case APPLIED -> {
                player.getInventory().setItemInMainHand(heldItem);
                cooldownSupplier.get().mark(player);
                player.closeInventory();
                announcerSupplier.get().announceVanilla(player);
            }
            case ALREADY_VANILLA -> langSupplier.get().send(player, "command.already-vanilla");
            case NO_SLOTS -> langSupplier.get().send(player, "command.no-slots");
            case NO_META -> langSupplier.get().send(player, "command.no-item-in-hand");
        }
    }

    private boolean notReady(Player player) {
        final CooldownService cooldown = cooldownSupplier.get();
        if (cooldown.isReady(player)) return false;
        langSupplier.get().send(player, "command.cooldown",
                "{seconds}", String.format("%.1f", cooldown.remainingMillis(player) / 1000.0));
        return true;
    }
}
