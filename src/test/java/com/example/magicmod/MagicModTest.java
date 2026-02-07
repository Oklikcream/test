package com.example.magicmod;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MagicModTest {

    @Test
    void scrollLearnIsPermanent() {
        SpellRegistry registry = new SpellRegistry();
        registry.register(new Spell("fireball", "Fireball", 20, 1));
        PlayerMagicProfile profile = new PlayerMagicProfile();

        SpellScroll scroll = new SpellScroll("fireball");
        assertTrue(scroll.learn(profile, registry));
        assertTrue(profile.hasSpell("fireball"));
        assertFalse(scroll.learn(profile, registry), "second learning should not be new");
    }

    @Test
    void arcaneWorkbenchSupportsSpellExplosionAndNothing() {
        ArcaneWorkbench workbench = new ArcaneWorkbench();
        String spellPattern = "a".repeat(81);
        String boomPattern = "b".repeat(81);
        workbench.registerSpellRecipe(spellPattern, "ice_spike");
        workbench.registerExplosionRecipe(boomPattern, "unstable runes");

        ArcaneCraftingResult spellResult = workbench.craft(spellPattern);
        assertEquals(ArcaneCraftingResult.ResultType.SPELL_SCROLL, spellResult.type());
        assertEquals("ice_spike", spellResult.spellId());

        ArcaneCraftingResult explosion = workbench.craft(boomPattern);
        assertEquals(ArcaneCraftingResult.ResultType.MAGIC_EXPLOSION, explosion.type());

        ArcaneCraftingResult nothing = workbench.craft("c".repeat(81));
        assertEquals(ArcaneCraftingResult.ResultType.NOTHING, nothing.type());
    }

    @Test
    void bindingAndCastingConsumesManaAndGivesExp() {
        SpellRegistry registry = new SpellRegistry();
        registry.register(new Spell("blink", "Blink", 15, 1));
        PlayerMagicProfile profile = new PlayerMagicProfile();
        profile.learnSpell("blink");
        profile.bindSpellToKey(1, "blink");

        SpellEngine engine = new SpellEngine(registry);
        int manaBefore = profile.currentMana();

        assertTrue(engine.castBoundSpell(profile, 1));
        assertTrue(profile.currentMana() < manaBefore);
        assertTrue(profile.magicExperience() > 0);
    }

    @Test
    void magicLevelRaisesManaPool() {
        PlayerMagicProfile profile = new PlayerMagicProfile();
        int oldMax = profile.maxMana();
        profile.grantExperience(10_000);
        assertTrue(profile.magicLevel() > 1);
        assertTrue(profile.maxMana() > oldMax);
    }

    @Test
    void bindingMenuModelSupportsUnbind() {
        PlayerMagicProfile profile = new PlayerMagicProfile();
        profile.learnSpell("blink");
        profile.bindSpellToKey(2, "blink");

        assertEquals("blink", profile.spellForKey(2));
        profile.unbindKey(2);
        assertNull(profile.spellForKey(2));
    }

    @Test
    void craftingNewSpellGivesExpOnlyOnce() {
        SpellRegistry registry = new SpellRegistry();
        registry.register(new Spell("fireball", "Fireball", 20, 1));
        SpellEngine engine = new SpellEngine(registry);
        PlayerMagicProfile profile = new PlayerMagicProfile();

        ArcaneCraftingResult craftResult = ArcaneCraftingResult.spell("fireball");
        assertTrue(engine.applyCraftingResult(profile, craftResult));
        int expAfterFirstCraft = profile.magicExperience();

        assertTrue(engine.applyCraftingResult(profile, craftResult));
        assertEquals(expAfterFirstCraft, profile.magicExperience());
    }
}
