package com.cesarcosmico.switchskin.item;

import com.cesarcosmico.switchskin.config.ItemConfig;
import com.cesarcosmico.switchskin.item.component.ComponentRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public final class ItemFactory {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private final Logger logger;
    private final ComponentRegistry components;

    public ItemFactory(Logger logger) {
        this.logger = logger;
        this.components = new ComponentRegistry(logger);
    }

    public ItemConfig parse(ConfigurationSection section, String fallbackMaterial) {
        if (section == null) {
            return new ItemConfig(new ItemStack(resolveMaterial(fallbackMaterial)), "", List.of());
        }
        final ItemStack baseItem = new ItemStack(resolveMaterial(section.getString("material", fallbackMaterial)));
        components.applyAll(baseItem, section);

        final String customNameRaw = section.getString("custom_name", "");
        final List<String> loreRaw = section.getStringList("lore");
        return new ItemConfig(baseItem, customNameRaw, loreRaw);
    }

    public ItemStack build(ItemConfig config) {
        return config.baseItem().clone();
    }

    public ItemStack build(ItemConfig config, Map<String, String> placeholders) {
        final ItemStack item = config.baseItem().clone();
        if (placeholders.isEmpty()) return item;
        if (!config.hasDynamicName() && !config.hasDynamicLore()) return item;

        final ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        if (config.hasDynamicName()) {
            meta.displayName(deserialize(replace(config.customNameRaw(), placeholders)));
        }
        if (config.hasDynamicLore()) {
            final List<Component> lore = config.loreRaw().stream()
                    .map(line -> replace(line, placeholders))
                    .map(ItemFactory::deserialize)
                    .toList();
            meta.lore(lore);
        }
        item.setItemMeta(meta);
        return item;
    }

    private Material resolveMaterial(String name) {
        final Material material = Material.matchMaterial(name);
        if (material != null) return material;
        logger.warning("Unknown material: " + name + ", using STONE");
        return Material.STONE;
    }

    private static String replace(String raw, Map<String, String> placeholders) {
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
