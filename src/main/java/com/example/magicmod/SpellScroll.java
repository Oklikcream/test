package com.example.magicmod;

import java.util.Objects;

public final class SpellScroll {
    private final String spellId;

    public SpellScroll(String spellId) {
        this.spellId = Objects.requireNonNull(spellId);
    }

    public String spellId() {
        return spellId;
    }

    public boolean learn(PlayerMagicProfile profile, SpellRegistry registry) {
        Spell spell = registry.get(spellId);
        if (spell == null) {
            return false;
        }
        return profile.learnSpell(spell.id());
    }
}
