package com.example.magicmod.fabric;

import com.example.magicmod.ArcaneCraftingResult;
import com.example.magicmod.ArcaneWorkbench;
import com.example.magicmod.PlayerMagicProfile;
import com.example.magicmod.Spell;
import com.example.magicmod.SpellEngine;
import com.example.magicmod.SpellRegistry;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.Comparator;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class MagicModFabric implements ModInitializer {
    public static final String MOD_ID = "magicmod";
    public static final Item SPELL_SCROLL_ITEM = new SpellScrollItem(new FabricItemSettings().maxCount(1));
    public static final Item RESEARCH_SCROLL_ITEM = new ResearchScrollItem(new FabricItemSettings().maxCount(1));

    public static final SpellRegistry SPELL_REGISTRY = new SpellRegistry();
    public static final SpellEngine SPELL_ENGINE = new SpellEngine(SPELL_REGISTRY);
    public static final ArcaneWorkbench WORKBENCH = new ArcaneWorkbench();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("spell_scroll"), SPELL_SCROLL_ITEM);
        Registry.register(Registries.ITEM, id("research_scroll"), RESEARCH_SCROLL_ITEM);

        SPELL_REGISTRY.register(new Spell("fireball", "Fireball", 20, 1));
        SPELL_REGISTRY.register(new Spell("blink", "Blink", 15, 1));
        SPELL_REGISTRY.register(new Spell("ice_spike", "Ice Spike", 25, 2));
        WORKBENCH.registerSpellRecipe("a".repeat(81), "fireball");
        WORKBENCH.registerSpellRecipe("c".repeat(81), "blink");
        WORKBENCH.registerSpellRecipe("d".repeat(81), "ice_spike");
        WORKBENCH.registerExplosionRecipe("b".repeat(81), "unstable_recipe");

        registerCommands();
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("magic")
                        .then(literal("give_scroll")
                                .then(argument("spell", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            String spellId = StringArgumentType.getString(ctx, "spell");
                                            ItemStack stack = SpellScrollItem.createForSpell(spellId);
                                            player.giveItemStack(stack);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Выдан свиток: " + spellId), false);
                                            return 1;
                                        })))
                        .then(literal("level").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            PlayerMagicProfile profile = MagicPlayerData.get(player);
                            ctx.getSource().sendFeedback(() -> Text.literal("Маг. уровень: " + profile.magicLevel() + ", мана: " + profile.currentMana() + "/" + profile.maxMana() + ", exp: " + profile.magicExperience() + "/" + profile.expToNextLevel()), false);
                            return 1;
                        }))
                        .then(literal("spells").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            PlayerMagicProfile profile = MagicPlayerData.get(player);
                            if (profile.learnedSpells().isEmpty()) {
                                player.sendMessage(Text.literal("У тебя пока нет изученных заклинаний.").formatted(Formatting.GRAY), false);
                                return 1;
                            }
                            player.sendMessage(Text.literal("Изученные заклинания:").formatted(Formatting.LIGHT_PURPLE), false);
                            SPELL_REGISTRY.all().stream()
                                    .filter(spell -> profile.hasSpell(spell.id()))
                                    .sorted(Comparator.comparing(Spell::id))
                                    .forEach(spell -> player.sendMessage(Text.literal("- " + spell.id() + " [" + spell.displayName() + "]").formatted(Formatting.AQUA), false));
                            return 1;
                        }))
                        .then(literal("bind")
                                .then(argument("key", IntegerArgumentType.integer(1, 9))
                                        .then(argument("spell", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                                    int key = IntegerArgumentType.getInteger(ctx, "key");
                                                    String spellId = StringArgumentType.getString(ctx, "spell");
                                                    PlayerMagicProfile profile = MagicPlayerData.get(player);
                                                    if (!profile.hasSpell(spellId)) {
                                                        player.sendMessage(Text.literal("Сначала изучи это заклинание: " + spellId).formatted(Formatting.RED), false);
                                                        return 0;
                                                    }
                                                    profile.bindSpellToKey(key, spellId);
                                                    player.sendMessage(Text.literal("Заклинание " + spellId + " привязано на слот " + key).formatted(Formatting.GREEN), false);
                                                    return 1;
                                                }))))
                        .then(literal("unbind")
                                .then(argument("key", IntegerArgumentType.integer(1, 9))
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            int key = IntegerArgumentType.getInteger(ctx, "key");
                                            PlayerMagicProfile profile = MagicPlayerData.get(player);
                                            profile.unbindKey(key);
                                            player.sendMessage(Text.literal("Слот " + key + " очищен.").formatted(Formatting.YELLOW), false);
                                            return 1;
                                        })))
                        .then(literal("bindings").executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                            PlayerMagicProfile profile = MagicPlayerData.get(player);
                            player.sendMessage(Text.literal("Бинды (слоты 1..9):").formatted(Formatting.GOLD), false);
                            for (int i = 1; i <= 9; i++) {
                                String bound = profile.spellForKey(i);
                                String line = "[" + i + "] " + (bound == null ? "пусто" : bound);
                                player.sendMessage(Text.literal(line), false);
                            }
                            return 1;
                        }))
                        .then(literal("cast")
                                .then(argument("key", IntegerArgumentType.integer(1, 9))
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            int key = IntegerArgumentType.getInteger(ctx, "key");
                                            PlayerMagicProfile profile = MagicPlayerData.get(player);
                                            boolean cast = SPELL_ENGINE.castBoundSpell(profile, key);
                                            if (cast) {
                                                player.sendMessage(Text.literal("Заклинание успешно применено из слота " + key + ".").formatted(Formatting.GREEN), false);
                                                return 1;
                                            }
                                            player.sendMessage(Text.literal("Не удалось применить заклинание (не изучено, не привязано, мало маны или низкий уровень).")
                                                    .formatted(Formatting.RED), false);
                                            return 0;
                                        })))
                        .then(literal("research")
                                .then(argument("pattern", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                            ArcaneCraftingResult result = WORKBENCH.craft(StringArgumentType.getString(ctx, "pattern"));
                                            if (result.type() == ArcaneCraftingResult.ResultType.SPELL_SCROLL) {
                                                player.giveItemStack(SpellScrollItem.createForSpell(result.spellId()));
                                                SPELL_ENGINE.applyCraftingResult(MagicPlayerData.get(player), result);
                                                player.sendMessage(Text.literal("Открыт новый свиток: " + result.spellId()), false);
                                            } else if (result.type() == ArcaneCraftingResult.ResultType.MAGIC_EXPLOSION) {
                                                player.getWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(), 2.0f, net.minecraft.world.World.ExplosionSourceType.MOB);
                                            } else {
                                                player.sendMessage(Text.literal("Рецепт не найден."), false);
                                            }
                                            return 1;
                                        })))
        ));
    }

    private static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
