package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.ItemConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import com.cesarcosmico.switchskin.config.TokenVisualConfig;
import com.cesarcosmico.switchskin.item.component.CustomModelDataApplier;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public final class TokenFactory {

    private final NamespacedKey key;
    private final Supplier<ItemConfig> tokenConfigSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final ItemFactory itemFactory;
    private final Function<SkinDefinition, @Nullable TokenVisualConfig> visualOverride;

    public TokenFactory(NamespacedKey key,
                        Supplier<ItemConfig> tokenConfigSupplier,
                        Supplier<SkinConfig> skinSupplier,
                        ItemFactory itemFactory,
                        Function<SkinDefinition, @Nullable TokenVisualConfig> visualOverride) {
        this.key = key;
        this.tokenConfigSupplier = tokenConfigSupplier;
        this.skinSupplier = skinSupplier;
        this.itemFactory = itemFactory;
        this.visualOverride = visualOverride;
    }

    public ItemStack create(String skinId, int amount) {
        final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        if (def == null) return null;

        final ItemConfig config = tokenConfigSupplier.get();
        final ItemStack item = itemFactory.build(config, Map.of("{skin}", def.nameOrId()));
        item.setAmount(Math.max(1, amount));

        applyVisualOverride(item, visualOverride.apply(def));

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, skinId);
        item.setItemMeta(meta);
        return item;
    }

    private static void applyVisualOverride(ItemStack item, @Nullable TokenVisualConfig override) {
        if (override == null) return;
        if (override.itemModel() != null) {
            final ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setItemModel(override.itemModel());
                item.setItemMeta(meta);
            }
        }
        if (override.customModelData() != null) {
            CustomModelDataApplier.applyTo(item, override.customModelData());
        }
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
