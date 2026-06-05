package com.cesarcosmico.switchskin.config.loader;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Set;
import java.util.logging.Logger;

public final class SchemaKeys {

    private SchemaKeys() {}

    public static void checkKeys(Logger logger, String file, String path, ConfigurationSection section,
                                 Set<String> allowed, Set<String> required) {
        if (section == null) return;
        for (String key : section.getKeys(false)) {
            if (!allowed.contains(key)) {
                logger.warning(file + ": unknown key '" + path + "." + key + "' (typo or removed?)");
            }
        }
        for (String r : required) {
            if (!section.isSet(r)) {
                logger.warning(file + ": missing required key '" + path + "." + r + "'");
            }
        }
    }
}
