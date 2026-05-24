package com.cesarcosmico.switchskin.listener;

import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI.MenuAction;
import com.cesarcosmico.switchskin.items.ItemFactory;
import com.cesarcosmico.switchskin.service.SkinAppearanceRenderer;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.cesarcosmico.switchskin.text.MessageManager;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class SkinMenuListener implements Listener {

    private final JavaPlugin plugin;
    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final PluginConfig pluginConfig;
    private final SkinSlotService service;
    private final SwitchAnnouncer announcer;
    private final ItemFactory itemFactory;
    private final SkinAppearanceRenderer appearanceRenderer;

    public SkinMenuListener(JavaPlugin plugin, MessageManager messages, SkinConfig skinConfig,
                            PluginConfig pluginConfig, SkinSlotService service, SwitchAnnouncer announcer,
                            ItemFactory itemFactory, SkinAppearanceRenderer appearanceRenderer) {
        this.plugin = plugin;
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.pluginConfig = pluginConfig;
        this.service = service;
        this.announcer = announcer;
        this.itemFactory = itemFactory;
        this.appearanceRenderer = appearanceRenderer;
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
            case MenuAction.SelectSkin select -> handleSkinSelect(player, gui, select.skinId());
            case MenuAction.SelectVanilla ignored -> handleVanilla(player, gui);
            case MenuAction.PrevPage ignored -> reopenAtPage(player, gui.getPage() - 1);
            case MenuAction.NextPage ignored -> reopenAtPage(player, gui.getPage() + 1);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!(event.getInventory().getHolder() instanceof SkinMenuGUI)) return;
        event.setCancelled(true);
    }

    private void handleSkinSelect(Player player, SkinMenuGUI gui, String skinId) {
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        final int targetIndex = service.getSlots(heldItem).indexOf(skinId);
        if (targetIndex < 0) {
            player.sendMessage(messages.getPrefixedMessage("command.skin-not-on-item",
                    Placeholder.parsed("skin", skinId)));
            player.closeInventory();
            return;
        }

        switch (service.selectIndex(heldItem, targetIndex)) {
            case SELECTED -> {
                player.getInventory().setItemInMainHand(heldItem);
                service.getActiveSkin(heldItem).ifPresent(s -> announcer.announceSwitch(player, s));
                reopenAtPage(player, gui.getPage());
            }
            case ALREADY_ACTIVE -> {
                final SkinDefinition def = skinConfig.get(skinId).orElse(null);
                final String display = def == null ? skinId : def.nameOrId();
                player.sendMessage(messages.getPrefixedMessage("command.already-active",
                        Placeholder.parsed("skin", display)));
            }
            case NO_SLOTS, INVALID_INDEX -> player.sendMessage(messages.getPrefixedMessage("command.no-slots"));
            case NO_META -> player.sendMessage(messages.getPrefixedMessage("command.no-item-in-hand"));
        }
    }

    private void handleVanilla(Player player, SkinMenuGUI gui) {
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        switch (service.selectVanilla(heldItem)) {
            case APPLIED -> {
                player.getInventory().setItemInMainHand(heldItem);
                announcer.announceVanilla(player);
                reopenAtPage(player, gui.getPage());
            }
            case ALREADY_VANILLA -> player.sendMessage(messages.getPrefixedMessage("command.already-vanilla"));
            case NO_SLOTS -> player.sendMessage(messages.getPrefixedMessage("command.no-slots"));
            case NO_META -> player.sendMessage(messages.getPrefixedMessage("command.no-item-in-hand"));
        }
    }

    private void reopenAtPage(Player player, int targetPage) {
        final ItemStack heldItem = player.getInventory().getItemInMainHand();
        if (!service.hasSlots(heldItem)) {
            player.sendMessage(messages.getPrefixedMessage("command.no-slots"));
            player.closeInventory();
            return;
        }
        final List<String> slots = service.getSlots(heldItem);
        final int activeIndex = service.getCurrentIndex(heldItem);
        final SkinMenuGUI next = new SkinMenuGUI(pluginConfig.getMenu(), skinConfig, itemFactory,
                appearanceRenderer, player.getUniqueId(), slots, activeIndex, targetPage, heldItem.getType());
        plugin.getServer().getScheduler().runTask(plugin, () -> next.open(player));
    }
}
