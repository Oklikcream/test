package com.example.magicmod.fabric;

import com.example.magicmod.PlayerMagicProfile;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsage;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.world.World;

public class ManaPotionItem extends Item {
    private static final int MANA_RESTORE = 45;

    public ManaPotionItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        return ItemUsage.consumeHeldItem(world, user, hand);
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        ItemStack result = super.finishUsing(stack, world, user);
        if (!world.isClient() && user instanceof ServerPlayerEntity player) {
            PlayerMagicProfile profile = MagicPlayerData.get(player);
            profile.regenerateMana(MANA_RESTORE);
            MagicModFabric.sendHudSyncPacket(player);
        }

        if (user instanceof PlayerEntity player && !player.getAbilities().creativeMode) {
            if (result.isEmpty()) {
                return new ItemStack(Items.GLASS_BOTTLE);
            }
            player.getInventory().insertStack(new ItemStack(Items.GLASS_BOTTLE));
        }
        return result;
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.DRINK;
    }
}
