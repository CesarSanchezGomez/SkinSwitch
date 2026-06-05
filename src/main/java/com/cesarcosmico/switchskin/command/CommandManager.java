package com.cesarcosmico.switchskin.command;

import com.cesarcosmico.switchskin.command.feature.AddSlotCommand;
import com.cesarcosmico.switchskin.command.feature.GiveTokenCommand;
import com.cesarcosmico.switchskin.command.feature.MenuCommand;
import com.cesarcosmico.switchskin.command.feature.ReloadCommand;
import com.cesarcosmico.switchskin.command.feature.RemoveSlotCommand;
import com.cesarcosmico.switchskin.command.feature.RemoveTooltipCommand;
import com.cesarcosmico.switchskin.command.feature.SwitchCommand;
import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.items.ItemFactory;
import com.cesarcosmico.switchskin.service.SkinAppearanceRenderer;
import com.cesarcosmico.switchskin.service.SkinSlotService;
import com.cesarcosmico.switchskin.service.SwitchAnnouncer;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class CommandManager {

    private final ReloadCommand reloadCommand;
    private final AddSlotCommand addSlotCommand;
    private final RemoveSlotCommand removeSlotCommand;
    private final GiveTokenCommand giveSkinTokenCommand;
    private final GiveTokenCommand giveTooltipTokenCommand;
    private final RemoveTooltipCommand removeTooltipCommand;
    private final MenuCommand menuCommand;
    private final SwitchCommand switchCommand;

    public CommandManager(MessageManager messages, SkinConfig skinConfig, PluginConfig pluginConfig,
                          SkinSlotService service, SwitchAnnouncer announcer,
                          ItemFactory itemFactory, SkinAppearanceRenderer appearanceRenderer,
                          Runnable reloadAction) {
        this.reloadCommand = new ReloadCommand(messages, reloadAction);
        this.addSlotCommand = new AddSlotCommand(messages, skinConfig, service);
        this.removeSlotCommand = new RemoveSlotCommand(messages, service);
        this.giveSkinTokenCommand = new GiveTokenCommand("givetokenskin", "command.token-given",
                GiveTokenCommand.Kind.SKIN, messages, skinConfig, pluginConfig, itemFactory);
        this.giveTooltipTokenCommand = new GiveTokenCommand("givetokentooltip", "command.tooltip-token-given",
                GiveTokenCommand.Kind.TOOLTIP, messages, skinConfig, pluginConfig, itemFactory);
        this.removeTooltipCommand = new RemoveTooltipCommand(messages, service);
        this.menuCommand = new MenuCommand(messages, skinConfig, pluginConfig, service,
                itemFactory, appearanceRenderer);
        this.switchCommand = new SwitchCommand(messages, skinConfig, service, announcer);
    }

    public LiteralCommandNode<CommandSourceStack> createCommand() {
        return build("switchskin");
    }

    public LiteralCommandNode<CommandSourceStack> createAliasCommand() {
        return build("ss");
    }

    private LiteralCommandNode<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(CommandSupport.permission("switchskin.use"))
                .executes(menuCommand::execute)
                .then(reloadCommand.create())
                .then(addSlotCommand.create())
                .then(removeSlotCommand.create())
                .then(giveSkinTokenCommand.create())
                .then(giveTooltipTokenCommand.create())
                .then(removeTooltipCommand.create())
                .then(menuCommand.create())
                .then(switchCommand.create())
                .build();
    }
}
