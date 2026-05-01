package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.PluginConfig;
import com.cesarcosmico.switchskin.config.SkinConfig;
import com.cesarcosmico.switchskin.config.SkinDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.function.Supplier;

public final class TokenFactory {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final NamespacedKey key;
    private final Supplier<PluginConfig.TokenConfig> tokenConfigSupplier;
    private final Supplier<SkinConfig> skinSupplier;
    private final Material fallbackMaterial;

    public TokenFactory(NamespacedKey key,
                        Supplier<PluginConfig.TokenConfig> tokenConfigSupplier,
                        Supplier<SkinConfig> skinSupplier,
                        Material fallbackMaterial) {
        this.key = key;
        this.tokenConfigSupplier = tokenConfigSupplier;
        this.skinSupplier = skinSupplier;
        this.fallbackMaterial = fallbackMaterial;
    }

    public ItemStack create(String skinId, int amount) {
        final SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        if (def == null) return null;

        final PluginConfig.TokenConfig token = tokenConfigSupplier.get();
        Material material = Material.matchMaterial(token.material());
        if (material == null) material = fallbackMaterial;

        final ItemStack item = new ItemStack(material, Math.max(1, amount));
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        final String label = def.nameOrId();
        if (!token.customName().isEmpty()) {
            meta.displayName(deserialize(token.customName().replace("{skin}", label)));
        }
        if (!token.lore().isEmpty()) {
            meta.lore(token.lore().stream()
                    .map(line -> line.replace("{skin}", label))
                    .map(TokenFactory::deserialize)
                    .toList());
        }

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

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
