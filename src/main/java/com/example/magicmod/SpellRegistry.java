package com.example.magicmod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class SpellRegistry {
    private final Map<String, Spell> spells = new HashMap<>();

    public void register(Spell spell) {
        spells.put(spell.id(), spell);
    }

    public Spell get(String id) {
        return spells.get(id);
    }


    public Collection<Spell> all() {
        return Collections.unmodifiableCollection(spells.values());
    }

}
