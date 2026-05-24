package com.cesarcosmico.switchskin.command;

import com.cesarcosmico.switchskin.text.MessageManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public final class CommandSupport {

    private CommandSupport() {}

    public static Predicate<CommandSourceStack> permission(String node) {
        return source -> source.getSender().hasPermission(node);
    }

    /** Returns the sender as a Player, or sends the only-players message and returns {@code null}. */
    @Nullable
    public static Player requirePlayer(CommandSourceStack source, MessageManager messages) {
        if (source.getSender() instanceof Player player) {
            return player;
        }
        send(source.getSender(), messages, "command.only-players");
        return null;
    }

    public static void send(CommandSender sender, MessageManager messages, String key) {
        sender.sendMessage(messages.getPrefixedMessage(key));
    }

    public static void send(CommandSender sender, MessageManager messages, String key, TagResolver resolver) {
        sender.sendMessage(messages.getPrefixedMessage(key, resolver));
    }
}
