package com.cesarcosmico.skinswitch.command;

import com.cesarcosmico.skinswitch.command.feature.AddSlotCommand;
import com.cesarcosmico.skinswitch.command.feature.GiveTokenCommand;
import com.cesarcosmico.skinswitch.command.feature.ReloadCommand;
import com.cesarcosmico.skinswitch.command.feature.RemoveSlotCommand;
import com.cesarcosmico.skinswitch.command.feature.RemoveTooltipCommand;
import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import com.cesarcosmico.skinswitch.item.TokenFactory;
import com.cesarcosmico.skinswitch.service.SkinSlotService;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.function.Supplier;

public final class CommandManager {

    private final ReloadCommand reloadCommand;
    private final AddSlotCommand addSlotCommand;
    private final RemoveSlotCommand removeSlotCommand;
    private final GiveTokenCommand giveSkinTokenCommand;
    private final GiveTokenCommand giveTooltipTokenCommand;
    private final RemoveTooltipCommand removeTooltipCommand;

    public CommandManager(Supplier<LangConfig> langSupplier,
                          Supplier<SkinConfig> skinSupplier,
                          Supplier<PluginConfig> pluginSupplier,
                          Supplier<SkinSlotService> serviceSupplier,
                          Supplier<TokenFactory> skinTokenSupplier,
                          Supplier<TokenFactory> tooltipTokenSupplier,
                          Runnable reloadAction) {
        this.reloadCommand = new ReloadCommand(langSupplier, reloadAction);
        this.addSlotCommand = new AddSlotCommand(langSupplier, skinSupplier, serviceSupplier);
        this.removeSlotCommand = new RemoveSlotCommand(langSupplier, serviceSupplier);
        this.giveSkinTokenCommand = new GiveTokenCommand("givetoken", "command.token-given",
                langSupplier, skinSupplier, skinTokenSupplier, def -> true);
        this.giveTooltipTokenCommand = new GiveTokenCommand("givetooltip", "command.tooltip-token-given",
                langSupplier, skinSupplier, tooltipTokenSupplier,
                (SkinDefinition def) -> def.tooltipStyle() != null);
        this.removeTooltipCommand = new RemoveTooltipCommand(langSupplier, serviceSupplier);
    }

    public LiteralCommandNode<CommandSourceStack> createCommand() {
        return build("skinswitch");
    }

    public LiteralCommandNode<CommandSourceStack> createAliasCommand() {
        return build("ss");
    }

    private LiteralCommandNode<CommandSourceStack> build(String name) {
        return Commands.literal(name)
                .requires(source -> source.getSender().hasPermission("skinswitch.use"))
                .then(reloadCommand.create())
                .then(addSlotCommand.create())
                .then(removeSlotCommand.create())
                .then(giveSkinTokenCommand.create())
                .then(giveTooltipTokenCommand.create())
                .then(removeTooltipCommand.create())
                .build();
    }
}
