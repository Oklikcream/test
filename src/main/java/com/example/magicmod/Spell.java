package com.example.magicmod;

import java.util.Objects;

public final class Spell {
    private final String id;
    private final String displayName;
    private final String description;
    private final int manaCost;

    public Spell(String id, String displayName, String description, int manaCost) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.description = Objects.requireNonNull(description);
        this.manaCost = manaCost;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public int manaCost() {
        return manaCost;
    }
}
