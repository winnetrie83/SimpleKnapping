package be.winnetrie.mod.simpleknapping.client.screen;

import net.minecraft.client.gui.GuiGraphics;

/** Small rendering helpers for the pre-extractor GUI API used by Minecraft 1.21.1. */
final class GuiCompat {
    private GuiCompat() {
    }

    static void outline(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        if (width <= 0 || height <= 0) {
            return;
        }
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y + 1, x + 1, y + height - 1, color);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }
}
