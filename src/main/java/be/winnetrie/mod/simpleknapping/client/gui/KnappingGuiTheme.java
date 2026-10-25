package be.winnetrie.mod.simpleknapping.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Shared, texture-free palette matching the admin screens. */
public final class KnappingGuiTheme {
    public static final int BACKDROP = 0xB0000000;
    public static final int PANEL = 0xFF171717;
    public static final int SURFACE = 0xFF292929;
    public static final int SURFACE_HOVER = 0xFF353535;
    public static final int BORDER = 0xFF6A6A6A;
    public static final int TEXT = 0xFFFFFFFF;
    public static final int MUTED = 0xFFAAAAAA;
    public static final int ACCENT = 0xFF83B7DB;

    private KnappingGuiTheme() { }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, PANEL);
        graphics.outline(x, y, width, height, BORDER);
    }

    public static void surface(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + height, SURFACE);
        graphics.outline(x, y, width, height, 0xFF555555);
    }

    public static void button(GuiGraphicsExtractor graphics, int x, int y, int width, int height,
                              boolean hovered, boolean active) {
        graphics.fill(x, y, x + width, y + height,
                active ? 0xFF31516B : hovered ? SURFACE_HOVER : SURFACE);
        graphics.outline(x, y, width, height,
                active ? ACCENT : hovered ? 0xFF888888 : 0xFF555555);
    }

    public static boolean hit(double mx, double my, int x, int y, int width, int height) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
}
