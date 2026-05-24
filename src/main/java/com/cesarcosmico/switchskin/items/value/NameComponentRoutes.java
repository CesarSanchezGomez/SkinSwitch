package com.cesarcosmico.switchskin.items.value;

import java.util.Set;

public final class NameComponentRoutes {

    private static final Set<String> ROUTES = Set.of(
            "custom_name",
            "item_name",
            "lore.*",
            "potion_contents.custom_name"
    );

    private NameComponentRoutes() {}

    public static boolean isMiniMessageRoute(String path) {
        if (ROUTES.contains(path)) return true;
        for (String pattern : ROUTES) {
            if (pattern.contains("*") && matchesWildcard(pattern, path)) return true;
        }
        return false;
    }

    private static boolean matchesWildcard(String pattern, String path) {
        String[] pParts = pattern.split("\\.");
        String[] tParts = path.split("\\.");
        if (pParts.length != tParts.length) return false;
        for (int i = 0; i < pParts.length; i++) {
            if (pParts[i].equals("*")) continue;
            if (!pParts[i].equals(tParts[i])) return false;
        }
        return true;
    }
}
