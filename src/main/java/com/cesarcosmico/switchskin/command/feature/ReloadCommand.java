package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.config.LangConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.function.Supplier;

public final class ReloadCommand {

    private final Supplier<LangConfig> langSupplier;
    private final Runnable reloadAction;

    public ReloadCommand(Supplier<LangConfig> langSupplier, Runnable reloadAction) {
        this.langSupplier = langSupplier;
        this.reloadAction = reloadAction;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("reload")
                .requires(source -> source.getSender().hasPermission("switchskin.admin"))
                .executes(this::execute)
                .build();
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        reloadAction.run();
        langSupplier.get().send(context.getSource().getSender(), "command.reload-success");
        return Command.SINGLE_SUCCESS;
    }
}
