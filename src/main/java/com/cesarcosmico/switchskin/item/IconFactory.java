package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.IconConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class IconFactory {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Logger logger;

    public IconFactory(Logger logger) {
        this.logger = logger;
    }

    public IconConfig parse(ConfigurationSection section, String fallbackMaterial) {
        if (section == null) {
            return new IconConfig(fallbackMaterial, "", List.of(), null);
        }
        final String material = section.getString("material", fallbackMaterial);
        final String customName = section.getString("custom_name", "");
        final List<String> lore = section.getStringList("lore");
        final String itemModelRaw = section.getString("item_model", null);
        final NamespacedKey itemModel = (itemModelRaw == null || itemModelRaw.isEmpty())
                ? null : NamespacedKey.fromString(itemModelRaw);
        return new IconConfig(material, customName, lore, itemModel);
    }

    public ItemStack build(IconConfig config) {
        return build(config, Map.of(), null);
    }

    public ItemStack build(IconConfig config, Map<String, String> placeholders, NamespacedKey itemModelOverride) {
        final ItemStack item = new ItemStack(resolveMaterial(config.material()));
        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (!config.customName().isEmpty()) {
            meta.displayName(deserialize(applyPlaceholders(config.customName(), placeholders)));
        }
        if (!config.lore().isEmpty()) {
            final List<Component> lore = config.lore().stream()
                    .map(line -> applyPlaceholders(line, placeholders))
                    .map(IconFactory::deserialize)
                    .toList();
            meta.lore(lore);
        }

        final NamespacedKey model = itemModelOverride != null ? itemModelOverride : config.itemModel();
        if (model != null) meta.setItemModel(model);

        item.setItemMeta(meta);
        return item;
    }

    private Material resolveMaterial(String name) {
        final Material material = Material.matchMaterial(name);
        if (material != null) return material;
        logger.warning("Unknown material: " + name + ", using STONE");
        return Material.STONE;
    }

    private static String applyPlaceholders(String raw, Map<String, String> placeholders) {
        if (placeholders.isEmpty()) return raw;
        String result = raw;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }

    private static Component deserialize(String raw) {
        return MINI.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }
}
