package com.example.magicmod.fabric;

import com.example.magicmod.ArcaneWorkbench;
import com.example.magicmod.PlayerMagicProfile;
import com.example.magicmod.SpellEngine;
import com.example.magicmod.SpellEngine.CastResult;
import com.example.magicmod.SpellRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class MagicModFabric implements ModInitializer {
    public static final String MOD_ID = "magicmod";

    public static final Identifier OPEN_MAGIC_UI_S2C = id("open_magic_ui");
    public static final Identifier OPEN_RESEARCH_UI_S2C = id("open_research_ui");
    public static final Identifier SYNC_MAGIC_HUD_S2C = id("sync_magic_hud");
    public static final Identifier BIND_SPELL_C2S = id("bind_spell");
    public static final Identifier CAST_SPELL_C2S = id("cast_spell");
    public static final Identifier SUBMIT_RESEARCH_C2S = id("submit_research");

    public static final Item SPELL_SCROLL_ITEM = new SpellScrollItem(new FabricItemSettings().maxCount(1));
    public static final Item RESEARCH_SCROLL_ITEM = new ResearchScrollItem(new FabricItemSettings().maxCount(1));
    public static final Item BLANK_SCROLL_ITEM = new BlankScrollItem(new FabricItemSettings().maxCount(64));

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
                sendHudSyncPacket(player);
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
                sendHudSyncPacket(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SUBMIT_RESEARCH_C2S, (server, player, handler, buf, sender) -> {
            String pattern = buf.readString();
            server.execute(() -> {
                if (pattern.length() != 81) {
                    return;
                }
                ArcaneCraftingResult result = WORKBENCH.craft(pattern);
                if (result.type() == ArcaneCraftingResult.ResultType.SPELL_SCROLL) {
                    player.giveItemStack(SpellScrollItem.createForSpell(result.spellId()));
                    SPELL_ENGINE.applyCraftingResult(MagicPlayerData.get(player), result);
                    consumeOneBlankScroll(player);
                    player.sendMessage(Text.literal("Создан свиток: " + result.spellId()).formatted(Formatting.GREEN), true);
                } else if (result.type() == ArcaneCraftingResult.ResultType.MAGIC_EXPLOSION) {
                    consumeOneBlankScroll(player);
                    player.getWorld().createExplosion(player, player.getX(), player.getY(), player.getZ(), 2.0f, World.ExplosionSourceType.MOB);
                } else {
                    player.sendMessage(Text.literal("Ничего не получилось."), true);
                }
                sendOpenUiPacket(player);
                sendHudSyncPacket(player);
            });
        });
    }

    private static void consumeOneBlankScroll(ServerPlayerEntity player) {
        if (player.isCreative()) {
            return;
        }
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isOf(BLANK_SCROLL_ITEM)) {
                stack.decrement(1);
                break;
            }
        }
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
            Spell spell = SPELL_REGISTRY.get(spellId);
            buf.writeString(spellId);
            buf.writeString(spell == null ? spellId : spell.displayName());
        }

        for (int i = 1; i <= 9; i++) {
            String bound = profile.spellForKey(i);
            buf.writeString(bound == null ? "" : bound);
        }

        ServerPlayNetworking.send(player, OPEN_MAGIC_UI_S2C, buf);
    }

    public static void sendOpenResearchUiPacket(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN_RESEARCH_UI_S2C, new PacketByteBuf(io.netty.buffer.Unpooled.buffer()));
    }

    public static void sendHudSyncPacket(ServerPlayerEntity player) {
        PlayerMagicProfile profile = MagicPlayerData.get(player);
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(profile.magicLevel());
        buf.writeInt(profile.currentMana());
        buf.writeInt(profile.maxMana());
        for (int i = 1; i <= 9; i++) {
            String bound = profile.spellForKey(i);
            buf.writeString(bound == null ? "" : bound);
        }
        ServerPlayNetworking.send(player, SYNC_MAGIC_HUD_S2C, buf);
    }

    private static String reasonForCastResult(CastResult cast) {
        return switch (cast) {
            case NOT_BOUND -> "слот не привязан";
            case NOT_LEARNED -> "заклинание не изучено";
            case UNKNOWN_SPELL -> "заклинание не найдено";
            case NOT_ENOUGH_MANA -> "не хватает маны";
            default -> "неизвестная ошибка";
        };
    }

    private static Identifier id(String path) {
        return Identifier.of(MOD_ID, path);
    }
}
