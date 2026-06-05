package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.gui.SkinMenuGUI;
import com.cesarcosmico.switchskin.items.ItemFactory;
import com.cesarcosmico.switchskin.service.SkinAppearanceRenderer;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class MenuCommand {

    private final MessageManager messages;
    private final SkinConfig skinConfig;
    private final PluginConfig pluginConfig;
    private final SkinSlotService service;
    private final ItemFactory itemFactory;
    private final SkinAppearanceRenderer appearanceRenderer;

    public MenuCommand(MessageManager messages, SkinConfig skinConfig, PluginConfig pluginConfig,
                       SkinSlotService service, ItemFactory itemFactory,
                       SkinAppearanceRenderer appearanceRenderer) {
        this.messages = messages;
        this.skinConfig = skinConfig;
        this.pluginConfig = pluginConfig;
        this.service = service;
        this.itemFactory = itemFactory;
        this.appearanceRenderer = appearanceRenderer;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("menu")
                .requires(CommandSupport.permission("switchskin.command.menu"))
                .executes(this::execute)
                .build();
    }

    public int execute(CommandContext<CommandSourceStack> ctx) {
        final Player player = CommandSupport.requirePlayer(ctx.getSource(), messages);
        if (player == null) return Command.SINGLE_SUCCESS;

        final ItemStack item = player.getInventory().getItemInMainHand();
        if (!service.hasSlots(item)) {
            CommandSupport.send(player, messages, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final List<String> slots = service.getSlots(item);
        if (slots.isEmpty()) {
            CommandSupport.send(player, messages, "command.no-slots");
            return Command.SINGLE_SUCCESS;
        }

        final int activeIndex = service.getCurrentIndex(item);
        new SkinMenuGUI(pluginConfig.getMenu(), skinConfig, itemFactory, appearanceRenderer,
                player.getUniqueId(), slots, activeIndex, 0, item.getType()).open(player);
        return Command.SINGLE_SUCCESS;
    }
}
