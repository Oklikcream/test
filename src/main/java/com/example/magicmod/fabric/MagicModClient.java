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
    private static final KeyBinding[] CAST_KEYS = new KeyBinding[9];
    private static MagicHudState hudState = MagicHudState.empty();

    @Override
    public void onInitializeClient() {
        for (int i = 0; i < 9; i++) {
            int glfwKey = GLFW.GLFW_KEY_1 + i;
            CAST_KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.magicmod.cast_" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    glfwKey,
                    "category.magicmod"
            ));
        }

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            for (int i = 0; i < 9; i++) {
                while (CAST_KEYS[i].wasPressed()) {
                    sendCastPacket(i + 1);
                }
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
        StringBuilder sb = new StringBuilder("[1-9] ");
        for (int i = 0; i < 9; i++) {
            String s = hudState.boundSlots().get(i);
            sb.append(i + 1).append(':').append(s.isBlank() ? '-' : s);
            if (i < 8) {
                sb.append("  ");
            }
        }
        context.drawText(client.textRenderer, Text.literal(sb.toString()), 8, 20, 0xFFFFFF, true);
    }
}
