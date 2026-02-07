package com.example.magicmod.fabric;

import com.example.magicmod.PlayerMagicProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Comparator;

public class BlankScrollItem extends Item {
    public BlankScrollItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!world.isClient && user instanceof ServerPlayerEntity serverPlayer) {
            PlayerMagicProfile profile = MagicPlayerData.get(serverPlayer);
            String spellId = profile.learnedSpells().stream().sorted(Comparator.naturalOrder()).findFirst().orElse(null);
            if (spellId == null) {
                user.sendMessage(Text.literal("Нет изученных заклинаний для записи в свиток."), true);
                return new TypedActionResult<>(ActionResult.FAIL, stack);
            }

            serverPlayer.giveItemStack(SpellScrollItem.createForSpell(spellId));
            if (!user.isCreative()) {
                stack.decrement(1);
            }
            user.sendMessage(Text.literal("Создан свиток заклинания: " + spellId), true);
            return new TypedActionResult<>(ActionResult.SUCCESS, stack);
        }
        return new TypedActionResult<>(ActionResult.SUCCESS, stack);
    }
}
