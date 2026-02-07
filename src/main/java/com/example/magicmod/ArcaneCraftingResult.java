package com.example.magicmod;

public record ArcaneCraftingResult(ResultType type, String spellId) {
    public enum ResultType {
        SPELL_SCROLL,
        MAGIC_EXPLOSION,
        NOTHING
    }

    public static ArcaneCraftingResult spell(String spellId) {
        return new ArcaneCraftingResult(ResultType.SPELL_SCROLL, spellId);
    }

    public static ArcaneCraftingResult explosion() {
        return new ArcaneCraftingResult(ResultType.MAGIC_EXPLOSION, null);
    }

    public static ArcaneCraftingResult nothing() {
        return new ArcaneCraftingResult(ResultType.NOTHING, null);
    }
}
