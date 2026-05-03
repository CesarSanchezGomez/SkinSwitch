package com.cesarcosmico.switchskin.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class CursorUtil {

    private CursorUtil() {}

    public static void consumeOne(Player player, ItemStack cursor) {
        final int newAmount = cursor.getAmount() - 1;
        if (newAmount <= 0) {
            player.setItemOnCursor(null);
            return;
        }
        final ItemStack reduced = cursor.clone();
        reduced.setAmount(newAmount);
        player.setItemOnCursor(reduced);
    }
}
