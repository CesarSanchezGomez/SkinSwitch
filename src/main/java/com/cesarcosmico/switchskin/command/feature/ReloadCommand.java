package com.cesarcosmico.switchskin.command.feature;

import com.cesarcosmico.switchskin.command.CommandSupport;
import com.cesarcosmico.switchskin.text.MessageManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

public final class ReloadCommand {

    private final MessageManager messages;
    private final Runnable reloadAction;

    public ReloadCommand(MessageManager messages, Runnable reloadAction) {
        this.messages = messages;
        this.reloadAction = reloadAction;
    }

    public LiteralCommandNode<CommandSourceStack> create() {
        return Commands.literal("reload")
                .requires(CommandSupport.permission("switchskin.command.reload"))
                .executes(this::execute)
                .build();
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        reloadAction.run();
        CommandSupport.send(context.getSource().getSender(), messages, "command.reload-success");
        return Command.SINGLE_SUCCESS;
    }
}
