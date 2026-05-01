package com.cesarcosmico.skinswitch.config;

import org.bukkit.NamespacedKey;

public record SkinDefinition(String id, NamespacedKey itemModel, String display, String icon) {}
