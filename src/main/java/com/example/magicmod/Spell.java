package com.example.magicmod;

import java.util.Objects;

public final class Spell {
    private final String id;
    private final String displayName;
    private final int manaCost;
    private final int levelRequirement;

    public Spell(String id, String displayName, int manaCost, int levelRequirement) {
        this.id = Objects.requireNonNull(id);
        this.displayName = Objects.requireNonNull(displayName);
        this.manaCost = manaCost;
        this.levelRequirement = levelRequirement;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int manaCost() {
        return manaCost;
    }

    public int levelRequirement() {
        return levelRequirement;
    }
}
