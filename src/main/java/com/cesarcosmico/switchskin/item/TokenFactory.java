package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.ItemConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class TokenFactory {

    private final NamespacedKey key;
    private final Supplier<ItemConfig> tokenConfigSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final ItemFactory itemFactory;

    public TokenFactory(NamespacedKey key,
                        Supplier<ItemConfig> tokenConfigSupplier,
                        Supplier<SkinConfig> skinSupplier,
                        ItemFactory itemFactory) {
        this.key = key;
        this.tokenConfigSupplier = tokenConfigSupplier;
        this.skinSupplier = skinSupplier;
        this.itemFactory = itemFactory;
    }

    public ItemStack create(String skinId, int amount) {
        final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        if (def == null) return null;

        final ItemConfig config = tokenConfigSupplier.get();
        final ItemStack item = itemFactory.build(config, Map.of("{skin}", def.nameOrId()));
        item.setAmount(Math.max(1, amount));

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skinId);
        item.setItemMeta(meta);
        return item;
    }

    public boolean is(ItemStack item) {
        if (item == null) return false;
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(key, PersistentDataType.STRING);
    }

    public Optional<String> readSkinId(ItemStack item) {
        if (item == null) return Optional.empty();
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        final String id = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }
}
