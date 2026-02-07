package com.example.magicmod.fabric;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class MagicModClient implements ClientModInitializer {
    private static KeyBinding prevSpellKey;
    private static KeyBinding nextSpellKey;
    private static KeyBinding castKey;
    private static MagicHudState hudState = MagicHudState.empty();
    private static int selectedSlot = 1;

    @Override
    public void onInitializeClient() {
        prevSpellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.magicmod.prev_spell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_COMMA, "category.magicmod"));
        nextSpellKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.magicmod.next_spell", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_PERIOD, "category.magicmod"));
        castKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.magicmod.cast_selected", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_R, "category.magicmod"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (prevSpellKey.wasPressed()) {
                selectedSlot = selectedSlot == 1 ? 9 : selectedSlot - 1;
            }
            while (nextSpellKey.wasPressed()) {
                selectedSlot = selectedSlot == 9 ? 1 : selectedSlot + 1;
            }
            while (castKey.wasPressed()) {
                sendCastPacket(selectedSlot);
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(MagicModFabric.OPEN_MAGIC_UI_S2C, (client, handler, buf, responseSender) -> {
            int level = buf.readInt();
            int mana = buf.readInt();
            int maxMana = buf.readInt();

            int learnedSize = buf.readInt();
            List<LearnedSpellEntry> learned = new ArrayList<>();
            for (int i = 0; i < learnedSize; i++) {
                String spellId = buf.readString();
                String displayName = buf.readString();
                learned.add(new LearnedSpellEntry(spellId, displayName));
            }

            List<String> bound = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                bound.add(buf.readString());
            }

            hudState = new MagicHudState(level, mana, maxMana, bound);
            MagicUiState state = new MagicUiState(level, mana, maxMana, learned, bound);
            client.execute(() -> MinecraftClient.getInstance().setScreen(new MagicBindScreen(state)));
        });

        ClientPlayNetworking.registerGlobalReceiver(MagicModFabric.OPEN_RESEARCH_UI_S2C, (client, handler, buf, responseSender) ->
                client.execute(() -> MinecraftClient.getInstance().setScreen(new ResearchScreen())));

        ClientPlayNetworking.registerGlobalReceiver(MagicModFabric.SYNC_MAGIC_HUD_S2C, (client, handler, buf, responseSender) -> {
            int level = buf.readInt();
            int mana = buf.readInt();
            int maxMana = buf.readInt();
            List<String> bound = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                bound.add(buf.readString());
            }
            hudState = new MagicHudState(level, mana, maxMana, bound);
        });

        HudRenderCallback.EVENT.register(this::renderHud);
    }

    private void sendCastPacket(int slot) {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeInt(slot);
        ClientPlayNetworking.send(MagicModFabric.CAST_SPELL_C2S, buf);
    }

    private void renderHud(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) {
            return;
        }
        context.drawText(client.textRenderer, Text.literal("Mana: " + hudState.mana() + "/" + hudState.maxMana() + " | Lv." + hudState.level()), 8, 8, 0x66CCFF, true);
        context.drawText(client.textRenderer, Text.literal("Spell: < / > | Cast: R | Selected slot: " + selectedSlot), 8, 20, 0xFFD37F, true);

        StringBuilder sb = new StringBuilder("Slots ");
        for (int i = 0; i < 9; i++) {
            String s = hudState.boundSlots().get(i);
            if (i + 1 == selectedSlot) {
                sb.append('[');
            }
            sb.append(i + 1).append(':').append(s.isBlank() ? '-' : s);
            if (i + 1 == selectedSlot) {
                sb.append(']');
            }
            if (i < 8) {
                sb.append("  ");
            }
        }
        context.drawText(client.textRenderer, Text.literal(sb.toString()), 8, 32, 0xFFFFFF, true);
    }
}
