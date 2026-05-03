package com.cesarcosmico.switchskin.item.component;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;
import java.util.logging.Logger;

public final class ProfileApplier implements ComponentApplier {

    private final Logger logger;

    public ProfileApplier(Logger logger) {
        this.logger = logger;
    }

    @Override
    public String key() {
        return "profile";
    }

    @Override
    public void apply(ItemStack item, ConfigurationSection section) {
        if (!section.contains(key())) return;

        if (item.getType() != Material.PLAYER_HEAD) {
            item.setType(Material.PLAYER_HEAD);
        }
        if (!(item.getItemMeta() instanceof SkullMeta skullMeta)) return;

        if (section.isConfigurationSection(key())) {
            applyFromSection(item, skullMeta, section.getConfigurationSection(key()));
        } else {
            applyFromName(item, skullMeta, section.getString(key()));
        }
    }

    private void applyFromSection(ItemStack item, SkullMeta meta, ConfigurationSection profileSection) {
        final String textures = profileSection.getString("textures");
        if (textures == null || textures.isEmpty()) return;
        try {
            final PlayerProfile profile = Bukkit.createProfile(UUID.randomUUID());
            profile.clearProperties();
            profile.setProperty(new ProfileProperty("textures", textures));
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        } catch (Exception e) {
            logger.warning("Invalid profile textures: " + e.getMessage());
        }
    }

    private void applyFromName(ItemStack item, SkullMeta meta, String name) {
        if (name == null || name.isEmpty()) return;
        try {
            final PlayerProfile profile = Bukkit.createProfile(name);
            meta.setPlayerProfile(profile);
            item.setItemMeta(meta);
        } catch (Exception e) {
            logger.warning("Invalid profile name: " + e.getMessage());
        }
    }
}
