package com.example.magicmod.fabric;

import com.example.magicmod.SpellScroll;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class SpellScrollItem extends Item {
    public static final String SPELL_ID_KEY = "spellId";

    public SpellScrollItem(Settings settings) {
        super(settings);
    }

    public static ItemStack createForSpell(String spellId) {
        ItemStack stack = new ItemStack(MagicModFabric.SPELL_SCROLL_ITEM);
        stack.getOrCreateNbt().putString(SPELL_ID_KEY, spellId);
        return stack;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        String spellId = stack.getOrCreateNbt().getString(SPELL_ID_KEY);

        if (spellId.isBlank()) {
            if (!world.isClient) {
                user.sendMessage(Text.literal("На свитке нет заклинания."), true);
            }
            return new TypedActionResult<>(ActionResult.FAIL, stack);
        }

        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            boolean learned = new SpellScroll(spellId).learn(MagicPlayerData.get(serverPlayer), MagicModFabric.SPELL_REGISTRY);
            if (learned) {
                user.sendMessage(Text.literal("Изучено заклинание: " + spellId), true);
                if (!user.isCreative()) {
                    stack.decrement(1);
                }
                return new TypedActionResult<>(ActionResult.SUCCESS, stack);
            }
            user.sendMessage(Text.literal("Заклинание уже изучено или не существует."), true);
        }
        return new TypedActionResult<>(ActionResult.PASS, stack);
    }
}
