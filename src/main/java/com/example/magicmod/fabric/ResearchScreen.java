package com.example.magicmod.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class ResearchScreen extends Screen {
    private final boolean[] cells = new boolean[25];

    public ResearchScreen() {
        super(Text.literal("Research 5x5"));
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 58;
        int top = this.height / 2 - 62;

        for (int i = 0; i < 25; i++) {
            int idx = i;
            int row = i / 5;
            int col = i % 5;
            int x = left + col * 22;
            int y = top + row * 22;
            this.addDrawableChild(ButtonWidget.builder(Text.literal("."), b -> {
                cells[idx] = !cells[idx];
                b.setMessage(Text.literal(cells[idx] ? "#" : "."));
            }).dimensions(x, y, 20, 20).build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Синтез"), b -> submit())
                .dimensions(left, top + 116, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Очистить"), b -> this.client.setScreen(new ResearchScreen()))
                .dimensions(left + 74, top + 116, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left + 148, top + 116, 70, 20).build());
    }

    private void submit() {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        StringBuilder pattern = new StringBuilder(25);
        for (boolean cell : cells) {
            pattern.append(cell ? 'x' : '_');
        }
        buf.writeString(pattern.toString());
        ClientPlayNetworking.send(MagicModFabric.SUBMIT_RESEARCH_C2S, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int left = this.width / 2 - 58;
        int top = this.height / 2 - 78;
        context.drawText(this.textRenderer, Text.literal("Blank Scroll: закрашивай 5x5 и жми Синтез"), left, top, 0xFFFFFF, false);
    }
}
