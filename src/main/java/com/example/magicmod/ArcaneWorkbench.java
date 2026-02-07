package com.example.magicmod;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ArcaneWorkbench {
    public static final int GRID_SIZE = 5;
    private final Map<String, String> spellRecipes = new HashMap<>();
    private final Map<String, String> explosionRecipes = new HashMap<>();

    public void registerSpellRecipe(String pattern25, String spellId) {
        validatePattern(pattern25);
        spellRecipes.put(pattern25, spellId);
    }

    public void registerExplosionRecipe(String pattern25, String reason) {
        validatePattern(pattern25);
        explosionRecipes.put(pattern25, reason);
    }

    public ArcaneCraftingResult craft(String pattern25) {
        validatePattern(pattern25);
        if (spellRecipes.containsKey(pattern25)) {
            return ArcaneCraftingResult.spell(spellRecipes.get(pattern25));
        }
        if (explosionRecipes.containsKey(pattern25)) {
            return ArcaneCraftingResult.explosion();
        }
        return ArcaneCraftingResult.nothing();
    }

    private static void validatePattern(String pattern25) {
        Objects.requireNonNull(pattern25, "pattern25");
        if (pattern25.length() != GRID_SIZE * GRID_SIZE) {
            throw new IllegalArgumentException("Pattern must have exactly 25 symbols for a 5x5 grid");
        }
    }
}
