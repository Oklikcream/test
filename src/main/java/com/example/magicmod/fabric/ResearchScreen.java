package com.example.magicmod.fabric;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.List;

public class ResearchScreen extends Screen {
    private enum RuneOption {
        EMPTY("Пусто", '_'),
        FIRE("Fireball", 'a'),
        CHAOS("Chaos", 'b'),
        BLINK("Blink", 'c'),
        ICE("Ice Spike", 'd');

        private final String label;
        private final char symbol;

        RuneOption(String label, char symbol) {
            this.label = label;
            this.symbol = symbol;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final RuneOption[] cells = new RuneOption[9];

    public ResearchScreen() {
        super(Text.literal("Research (9 selectors)"));
        for (int i = 0; i < cells.length; i++) {
            cells[i] = RuneOption.EMPTY;
        }
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 130;
        int top = this.height / 2 - 75;

        for (int i = 0; i < 9; i++) {
            int idx = i;
            int row = i / 3;
            int col = i % 3;
            int x = left + col * 88;
            int y = top + row * 24;

            this.addDrawableChild(CyclingButtonWidget.builder(option -> Text.literal(option.toString()))
                    .values(List.of(RuneOption.values()))
                    .initially(cells[idx])
                    .build(x, y, 84, 20, Text.literal("S" + (i + 1)), (button, value) -> cells[idx] = value));
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Синтез"), b -> submit())
                .dimensions(left, top + 84, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Сброс"), b -> this.client.setScreen(new ResearchScreen()))
                .dimensions(left + 88, top + 84, 80, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(left + 176, top + 84, 80, 20).build());
    }

    private void submit() {
        PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
        buf.writeString(buildPattern25());
        ClientPlayNetworking.send(MagicModFabric.SUBMIT_RESEARCH_C2S, buf);
    }

    private String buildPattern25() {
        char[] pattern = new char[25];
        java.util.Arrays.fill(pattern, '_');
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int patternIndex = (row + 1) * 5 + (col + 1);
            pattern[patternIndex] = cells[i].symbol;
        }
        return new String(pattern);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int left = this.width / 2 - 130;
        int top = this.height / 2 - 94;
        context.drawText(this.textRenderer, Text.literal("Research: 9 выпадающих списков заклинаний"), left, top, 0xFFFFFF, false);
        context.drawText(this.textRenderer, Text.literal("Нужен 1 blank_scroll на синтез"), left, top + 12, 0xAAAAAA, false);
    }
}
