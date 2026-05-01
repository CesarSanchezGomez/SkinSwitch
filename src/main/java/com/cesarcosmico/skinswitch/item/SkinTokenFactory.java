package com.cesarcosmico.skinswitch.item;

import com.cesarcosmico.skinswitch.config.PluginConfig;
import com.cesarcosmico.skinswitch.config.SkinConfig;
import com.cesarcosmico.skinswitch.config.SkinDefinition;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;
import java.util.function.Supplier;

public final class SkinTokenFactory {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final SkinSlotKeys keys;
    private final Supplier<PluginConfig> pluginSupplier;
    private final Supplier<SkinConfig> skinSupplier;

    public SkinTokenFactory(SkinSlotKeys keys,
                            Supplier<PluginConfig> pluginSupplier,
                            Supplier<SkinConfig> skinSupplier) {
        this.keys = keys;
        this.pluginSupplier = pluginSupplier;
        this.skinSupplier = skinSupplier;
    }

    public ItemStack create(String skinId, int amount) {
        SkinDefinition def = skinSupplier.get().get(skinId).orElse(null);
        if (def == null) return null;

        PluginConfig.TokenConfig token = pluginSupplier.get().getToken();
        Material material = Material.matchMaterial(token.material());
        if (material == null) material = Material.NAME_TAG;

        ItemStack item = new ItemStack(material, Math.max(1, amount));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        String label = def.displayOrId();
        if (!token.customName().isEmpty()) {
            meta.displayName(deserialize(token.customName().replace("{skin}", label)));
        }
        if (!token.lore().isEmpty()) {
            meta.lore(token.lore().stream()
                    .map(line -> line.replace("{skin}", label))
                    .map(SkinTokenFactory::deserialize)
                    .toList());
        }

        meta.getPersistentDataContainer().set(keys.tokenSkin(), PersistentDataType.STRING, skinId);
        item.setItemMeta(meta);
        return item;
    }

    public boolean isToken(ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(keys.tokenSkin(), PersistentDataType.STRING);
    }

    public Optional<String> readSkinId(ItemStack item) {
        if (item == null) return Optional.empty();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        String id = meta.getPersistentDataContainer().get(keys.tokenSkin(), PersistentDataType.STRING);
        return Optional.ofNullable(id);
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
