package com.cesarcosmico.switchskin.domain.id;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record SkinId(@NotNull String value) {

    public SkinId {
        Objects.requireNonNull(value, "SkinId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("SkinId value must not be blank");
        }
    }

    public static SkinId of(@NotNull String value) {
        return new SkinId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
