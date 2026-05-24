package com.cesarcosmico.switchskin.listener;

import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.items.PluginItemTag;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.cesarcosmico.switchskin.util.CursorUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public final class SkinTokenListener implements Listener {

    private static final String TOKEN_USE_PERMISSION = "switchskin.token.use";

    private final SkinSlotService skinSlotService;
    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final SwitchAnnouncer announcer;

    public SkinTokenListener(SkinSlotService skinSlotService, MessageManager messages,
                             SkinConfig skinConfig, SwitchAnnouncer announcer) {
        this.skinSlotService = skinSlotService;
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.announcer = announcer;
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

        if (PluginItemTag.readSkinToken(cursor).isPresent()) {
            if (isToken(target)) return;
            event.setCancelled(true);
            if (!player.hasPermission(TOKEN_USE_PERMISSION)) return;
            handleSkinToken(player, cursor, target, PluginItemTag.readSkinToken(cursor).orElseThrow());
            return;
        }
        if (PluginItemTag.readTooltipToken(cursor).isPresent()) {
            if (isToken(target)) return;
            event.setCancelled(true);
            if (!player.hasPermission(TOKEN_USE_PERMISSION)) return;
            handleTooltipToken(player, cursor, target, PluginItemTag.readTooltipToken(cursor).orElseThrow());
        }
    }

    private static boolean isToken(ItemStack item) {
        return PluginItemTag.readSkinToken(item).isPresent()
                || PluginItemTag.readTooltipToken(item).isPresent();
    }

    private void handleSkinToken(Player player, ItemStack cursor, ItemStack target, String skinId) {
        final SkinSlotService.AddResult result = skinSlotService.addSlot(target, skinId, player);
        final SkinDefinition def = skinConfig.get(skinId).orElse(null);
        final String display = def == null ? skinId : def.nameOrId();
        switch (result) {
            case ADDED -> {
                CursorUtil.consumeOne(player, cursor);
                announcer.playTokenSound(player);
                send(player, "command.slot-added", TagResolver.resolver(
                        Placeholder.parsed("skin", display),
                        Placeholder.unparsed("count", String.valueOf(skinSlotService.getSlots(target).size()))));
            }
            case DUPLICATE -> send(player, "command.duplicate-slot");
            case FULL -> send(player, "command.slots-full",
                    Placeholder.unparsed("max", String.valueOf(skinSlotService.getSlots(target).size())));
            case INCOMPATIBLE -> send(player, "command.skin-incompatible", TagResolver.resolver(
                    Placeholder.parsed("skin", display),
                    Placeholder.unparsed("material", target.getType().getKey().value())));
            case UNKNOWN_SKIN -> send(player, "command.unknown-skin", Placeholder.parsed("skin", skinId));
            case NO_META -> { }
        }
    }

    private void handleTooltipToken(Player player, ItemStack cursor, ItemStack target, String skinId) {
        final SkinSlotService.TooltipApplyResult result = skinSlotService.applyTooltip(target, skinId);
        final SkinDefinition def = skinConfig.get(skinId).orElse(null);
        final String display = def == null ? skinId : def.nameOrId();
        switch (result) {
            case APPLIED -> {
                CursorUtil.consumeOne(player, cursor);
                announcer.playTokenSound(player);
                send(player, "command.tooltip-applied", Placeholder.parsed("skin", display));
            }
            case NO_SKIN_SLOT -> send(player, "command.tooltip-needs-slot", Placeholder.parsed("skin", display));
            case ALREADY_APPLIED -> send(player, "command.tooltip-duplicate", Placeholder.parsed("skin", display));
            case NO_TOOLTIP -> send(player, "command.tooltip-missing", Placeholder.parsed("skin", skinId));
            case UNKNOWN_SKIN -> send(player, "command.unknown-skin", Placeholder.parsed("skin", skinId));
            case NO_META -> { }
        }
    }

    private void send(CommandSender sender, String key) {
        sender.sendMessage(messages.getPrefixedMessage(key));
    }

    private void send(CommandSender sender, String key, TagResolver resolver) {
        sender.sendMessage(messages.getPrefixedMessage(key, resolver));
    }
}
