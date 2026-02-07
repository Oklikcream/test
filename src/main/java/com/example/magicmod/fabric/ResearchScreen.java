package com.example.magicmod.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class ResearchScreen extends Screen {
    private static final char[] SYMBOLS = new char[]{'_', 'a', 'b', 'c', 'd'};
    private final char[] grid = new char[81];

    public ResearchScreen() {
        super(Text.literal("Research 9x9"));
        for (int i = 0; i < grid.length; i++) {
            grid[i] = '_';
        }
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 92;
        int top = this.height / 2 - 92;
        int cell = 18;

        for (int i = 0; i < 81; i++) {
            int idx = i;
            int row = i / 9;
            int col = i % 9;
            int x = left + col * cell;
            int y = top + row * cell;
            this.addDrawableChild(ButtonWidget.builder(Text.literal(String.valueOf(grid[idx])), b -> {
                grid[idx] = nextSymbol(grid[idx]);
                b.setMessage(Text.literal(String.valueOf(grid[idx])));
            }).dimensions(x, y, 16, 16).build());
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Синтез"), b -> submit())
                .dimensions(left, top + 9 * cell + 8, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Очистить"), b -> this.client.setScreen(new ResearchScreen()))
                .dimensions(left + 76, top + 9 * cell + 8, 70, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left + 152, top + 9 * cell + 8, 70, 20).build());
    }

    private void submit() {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeString(new String(grid));
        ClientPlayNetworking.send(MagicModFabric.SUBMIT_RESEARCH_C2S, buf);
    }

    private static char nextSymbol(char current) {
        for (int i = 0; i < SYMBOLS.length; i++) {
            if (SYMBOLS[i] == current) {
                return SYMBOLS[(i + 1) % SYMBOLS.length];
            }
        }
        return SYMBOLS[0];
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int left = this.width / 2 - 92;
        int top = this.height / 2 - 108;
        context.drawText(this.textRenderer, Text.literal("Поле исследования 9x9 (символы _,a,b,c,d)"), left, top, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("a*81=fireball, c*81=blink, d*81=ice_spike, b*81=взрыв"), left, top + 12, 0xAAAAAA, false);
    }
}
