package com.example.magicmod.fabric;

import com.example.magicmod.ArcaneWorkbench;
import com.example.magicmod.PlayerMagicProfile;
import com.example.magicmod.Spell;
import com.example.magicmod.SpellEngine;
import com.example.magicmod.SpellEngine.CastResult;
import com.example.magicmod.SpellRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class MagicModFabric implements ModInitializer {
    public static final String MOD_ID = "magicmod";

    public static final Identifier OPEN_MAGIC_UI_S2C = id("open_magic_ui");
    public static final Identifier BIND_SPELL_C2S = id("bind_spell");
    public static final Identifier CAST_SPELL_C2S = id("cast_spell");

    public static final Item SPELL_SCROLL_ITEM = new SpellScrollItem(new FabricItemSettings().maxCount(1));
    public static final Item RESEARCH_SCROLL_ITEM = new ResearchScrollItem(new FabricItemSettings().maxCount(1));
    public static final Item BLANK_SCROLL_ITEM = new Item(new FabricItemSettings().maxCount(64));

    public static final SpellRegistry SPELL_REGISTRY = new SpellRegistry();
    public static final SpellEngine SPELL_ENGINE = new SpellEngine(SPELL_REGISTRY);
    public static final ArcaneWorkbench WORKBENCH = new ArcaneWorkbench();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM, id("spell_scroll"), SPELL_SCROLL_ITEM);
        Registry.register(Registries.ITEM, id("research_scroll"), RESEARCH_SCROLL_ITEM);
        Registry.register(Registries.ITEM, id("blank_scroll"), BLANK_SCROLL_ITEM);

        SPELL_REGISTRY.register(new Spell("fireball", "Fireball", 20, 1));
        SPELL_REGISTRY.register(new Spell("blink", "Blink", 15, 1));
        SPELL_REGISTRY.register(new Spell("ice_spike", "Ice Spike", 25, 2));
        WORKBENCH.registerSpellRecipe("a".repeat(81), "fireball");
        WORKBENCH.registerSpellRecipe("c".repeat(81), "blink");
        WORKBENCH.registerSpellRecipe("d".repeat(81), "ice_spike");
        WORKBENCH.registerExplosionRecipe("b".repeat(81), "unstable_recipe");

        registerPackets();
    }

    private static void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(BIND_SPELL_C2S, (server, player, handler, buf, sender) -> {
            int slot = buf.readInt();
            String spellId = buf.readString();
            server.execute(() -> {
                PlayerMagicProfile profile = MagicPlayerData.get(player);
                if (slot < 1 || slot > 9) {
                    return;
                }
                if (spellId.isBlank()) {
                    profile.unbindKey(slot);
                } else if (profile.hasSpell(spellId)) {
                    profile.bindSpellToKey(slot, spellId);
                }
                sendOpenUiPacket(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(CAST_SPELL_C2S, (server, player, handler, buf, sender) -> {
            int slot = buf.readInt();
            server.execute(() -> {
                PlayerMagicProfile profile = MagicPlayerData.get(player);
                String spellId = profile.spellForKey(slot);
                CastResult cast = SPELL_ENGINE.castBoundSpellDetailed(profile, slot);
                if (cast == CastResult.SUCCESS && spellId != null) {
                    SpellEffects.cast(player, spellId);
                } else if (cast != CastResult.SUCCESS) {
                    player.sendMessage(Text.literal("Каст не удался: " + reasonForCastResult(cast)).formatted(Formatting.RED), true);
                }
                sendOpenUiPacket(player);
            });
        });
    }

    public static void sendOpenUiPacket(ServerPlayerEntity player) {
        PlayerMagicProfile profile = MagicPlayerData.get(player);
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());

        buf.writeInt(profile.magicLevel());
        buf.writeInt(profile.currentMana());
        buf.writeInt(profile.maxMana());

        List<String> learned = new ArrayList<>(profile.learnedSpells());
        learned.sort(String::compareTo);
        buf.writeInt(learned.size());
        for (String spellId : learned) {
            buf.writeString(spellId);
        }

        for (int i = 1; i <= 9; i++) {
            String bound = profile.spellForKey(i);
            buf.writeString(bound == null ? "" : bound);
        }

        ServerPlayNetworking.send(player, OPEN_MAGIC_UI_S2C, buf);
    }

    private static String reasonForCastResult(CastResult cast) {
        return switch (cast) {
            case NOT_BOUND -> "слот не привязан";
            case NOT_LEARNED -> "заклинание не изучено";
            case UNKNOWN_SPELL -> "заклинание не найдено";
            case LEVEL_TOO_LOW -> "недостаточный уровень";
            case NOT_ENOUGH_MANA -> "не хватает маны";
            default -> "неизвестная ошибка";
        };
    }

    private static void sendSpellList(ServerPlayerEntity player, PlayerMagicProfile profile) {
        if (profile.learnedSpells().isEmpty()) {
            player.sendMessage(Text.literal("У тебя пока нет изученных заклинаний.").formatted(Formatting.GRAY), false);
            return;
        }
        player.sendMessage(Text.literal("Изученные заклинания:").formatted(Formatting.LIGHT_PURPLE), false);
        SPELL_REGISTRY.all().stream()
                .filter(spell -> profile.hasSpell(spell.id()))
                .sorted(Comparator.comparing(Spell::id))
                .forEach(spell -> player.sendMessage(Text.literal("- " + spell.id() + " [" + spell.displayName() + "]").formatted(Formatting.AQUA), false));
    }

    private static void showBindings(ServerPlayerEntity player, PlayerMagicProfile profile) {
        player.sendMessage(Text.literal("Бинды (слоты 1..9):").formatted(Formatting.GOLD), false);
        for (int i = 1; i <= 9; i++) {
            String bound = profile.spellForKey(i);
            String line = "[" + i + "] " + (bound == null ? "пусто" : bound);
            player.sendMessage(Text.literal(line), false);
        }
    }

    private static void sendBindingInterface(ServerPlayerEntity player, PlayerMagicProfile profile) {
        player.sendMessage(Text.literal("=== Магическое меню (чат-интерфейс) ===").formatted(Formatting.LIGHT_PURPLE), false);
        player.sendMessage(Text.literal("[Обновить бинды]").styled(style -> style
                .withColor(Formatting.GOLD)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/magic bindings"))), false);

        if (profile.learnedSpells().isEmpty()) {
            player.sendMessage(Text.literal("Изучи свиток, чтобы появились варианты бинда.").formatted(Formatting.GRAY), false);
            return;
        }

        player.sendMessage(Text.literal("Нажми на слот для привязки:").formatted(Formatting.AQUA), false);
        profile.learnedSpells().stream().sorted().forEach(spellId -> {
            MutableText row = Text.literal(spellId + " -> ");
            for (int slot = 1; slot <= 9; slot++) {
                int bindSlot = slot;
                row.append(Text.literal("[" + slot + "]").styled(style -> style
                        .withColor(Formatting.GREEN)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/magic bind " + bindSlot + " " + spellId))));
                if (slot < 9) {
                    row.append(Text.literal(" "));
                }
            }
            player.sendMessage(row, false);
        });
    }

    private static String reasonForCastResult(CastResult cast) {
        return switch (cast) {
            case NOT_BOUND -> "На этом слоте нет бинда.";
            case NOT_LEARNED -> "Заклинание привязано, но еще не изучено.";
            case UNKNOWN_SPELL -> "Заклинание не найдено в реестре.";
            case LEVEL_TOO_LOW -> "Недостаточный уровень магии.";
            case NOT_ENOUGH_MANA -> "Недостаточно маны.";
            default -> "Неизвестная ошибка каста.";
        };
    }

    private static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
