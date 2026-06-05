package com.cesarcosmico.switchskin.domain.id;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public record PlayerId(@NotNull UUID value) {

    public PlayerId {
        Objects.requireNonNull(value, "PlayerId value must not be null");
    }

    public static PlayerId of(@NotNull UUID uuid) {
        return new PlayerId(uuid);
    }

    public static PlayerId fromString(@NotNull String raw) {
        return new PlayerId(UUID.fromString(raw));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
