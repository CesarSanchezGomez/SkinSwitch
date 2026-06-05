package com.cesarcosmico.switchskin.items.value;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ValueCompiler {

    private ValueCompiler() {}

    public static Map<String, ComponentValue> compileComponents(ConfigurationSection section) {
        Map<String, ComponentValue> out = new LinkedHashMap<>();
        if (section == null) return out;
        for (String rawKey : section.getKeys(false)) {
            String key = canonicalize(rawKey);
            out.put(key, compile(section.get(rawKey), key));
        }
        return out;
    }

    private static ComponentValue compile(Object raw, String path) {
        if (raw instanceof ConfigurationSection section) {
            return compileMap(section.getValues(false), path);
        }
        if (raw instanceof Map<?, ?> map) {
            return compileMap(coerceMap(map), path);
        }
        if (raw instanceof List<?> list) {
            String childPath = path.isEmpty() ? "*" : path + ".*";
            List<ComponentValue> children = new ArrayList<>(list.size());
            for (Object element : list) {
                children.add(compile(element, childPath));
            }
            return new ListValue(children);
        }
        if (raw instanceof String s && NameComponentRoutes.isMiniMessageRoute(path)) {
            return new MiniMessageStringValue(s);
        }
        return new LiteralValue(raw);
    }

    private static MapValue compileMap(Map<String, Object> map, String basePath) {
        Map<String, ComponentValue> children = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String childKey = canonicalize(entry.getKey());
            String childPath = basePath.isEmpty() ? childKey : basePath + "." + childKey;
            children.put(childKey, compile(entry.getValue(), childPath));
        }
        return new MapValue(children);
    }

    private static String canonicalize(String key) {
        int colon = key.indexOf(':');
        if (colon < 0) {
            return key.replace('-', '_');
        }
        String namespace = key.substring(0, colon);
        String name = key.substring(colon + 1);
        if (namespace.equals("minecraft")) {
            return name.replace('-', '_');
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> coerceMap(Map<?, ?> raw) {
        Map<String, Object> out = new LinkedHashMap<>(raw.size());
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                out.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return out;
    }
}
