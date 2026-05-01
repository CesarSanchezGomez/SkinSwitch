package com.cesarcosmico.skinswitch.command;

import com.cesarcosmico.skinswitch.command.feature.AddSlotCommand;
import com.cesarcosmico.skinswitch.command.feature.GiveTokenCommand;
import com.cesarcosmico.skinswitch.command.feature.ReloadCommand;
import com.cesarcosmico.skinswitch.command.feature.RemoveSlotCommand;
import com.cesarcosmico.skinswitch.config.LangConfig;
import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.item.SkinTokenFactory;
import com.cesarcosmico.skinswitch.service.SkinSlotService;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.function.Supplier;

public final class CommandManager {

    private final ReloadCommand reloadCommand;
    private final AddSlotCommand addSlotCommand;
    private final RemoveSlotCommand removeSlotCommand;
    private final GiveTokenCommand giveTokenCommand;

    public CommandManager(Supplier<LangConfig> langSupplier,
                          Supplier<SkinConfig> skinSupplier,
                          Supplier<PluginConfig> pluginSupplier,
                          Supplier<SkinSlotService> serviceSupplier,
                          Supplier<SkinTokenFactory> tokenSupplier,
                          Runnable reloadAction) {
        this.reloadCommand = new ReloadCommand(langSupplier, reloadAction);
        this.addSlotCommand = new AddSlotCommand(langSupplier, skinSupplier, serviceSupplier);
        this.removeSlotCommand = new RemoveSlotCommand(langSupplier, serviceSupplier);
        this.giveTokenCommand = new GiveTokenCommand(langSupplier, skinSupplier, tokenSupplier);
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
                .then(giveTokenCommand.create())
                .build();
    }
}
