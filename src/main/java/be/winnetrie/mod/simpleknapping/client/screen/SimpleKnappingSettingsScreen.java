package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.network.SettingsPayload;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

/** Dedicated admin/OP settings GUI opened through /simpleknapping settings. */
@SuppressWarnings("null")
public final class SimpleKnappingSettingsScreen extends Screen {
    // Match the compact Recipe Manager and Type Manager exactly.
    private static final int PANEL_W = 580;
    private static final int PANEL_H = 320;
    private static final int ROW_X_OFFSET = 18;
    private static final int ROW_W = 544;
    private static final int ROW_H = 48;
    private static final int TOGGLE_W = 72;
    private static final int TOGGLE_H = 22;

    private boolean woodenTools;
    private boolean stoneTools;
    private boolean treePunching;
    private String notice;
    private boolean noticeError;

    public SimpleKnappingSettingsScreen(boolean woodenTools,
                                        boolean stoneTools,
                                        boolean treePunching,
                                        String notice,
                                        boolean noticeError) {
        super(Component.literal("Simple Knapping Settings"));
        this.woodenTools = woodenTools;
        this.stoneTools = stoneTools;
        this.treePunching = treePunching;
        this.notice = notice == null ? "" : notice;
        this.noticeError = noticeError;
    }

    public void applyServerSnapshot(boolean woodenTools,
                                    boolean stoneTools,
                                    boolean treePunching,
                                    String notice,
                                    boolean noticeError) {
        this.woodenTools = woodenTools;
        this.stoneTools = stoneTools;
        this.treePunching = treePunching;
        this.notice = notice == null ? "" : notice;
        this.noticeError = noticeError;
    }

    /**
     * Minecraft 1.21.1 can apply the vanilla accessibility background blur
     * from Screen#renderBackground. This custom screen already renders its
     * own backdrop, so skip that pass to keep the GUI sharp.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Intentionally empty: render() draws this screen's background.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int right = left + Math.min(PANEL_W, this.width - 12);
        int bottom = top + Math.min(PANEL_H, this.height - 12);

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        GuiCompat.outline(graphics, left, top, right - left, bottom - top, 0xFF6A6A6A);

        graphics.drawString(this.font, Component.literal("Simple Knapping Settings"), left + 14, top + 12, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.literal("Server progression features"), left + 14, top + 27, 0xFF9E9E9E, false);

        drawFeatureRow(
                graphics, mouseX, mouseY,
                left + ROW_X_OFFSET, top + 55,
                "Wooden tools & weapons",
                "Disabled tier is replaced by flint in survival sources.",
                woodenTools
        );
        drawFeatureRow(
                graphics, mouseX, mouseY,
                left + ROW_X_OFFSET, top + 111,
                "Stone tools & weapons",
                "Disabled tier is replaced by flint in survival sources.",
                stoneTools
        );
        drawFeatureRow(
                graphics, mouseX, mouseY,
                left + ROW_X_OFFSET, top + 167,
                "Tree punching",
                "Logs may be broken without an axe.",
                treePunching
        );

        graphics.drawString(this.font,
                Component.literal("Instant: crafting stays empty; loot/trades/containers use flint replacements."),
                left + 18, top + 230, 0xFF8E8E8E, false);

        if (!notice.isBlank()) {
            graphics.drawWordWrap(
                    this.font,
                    Component.literal(notice),
                    left + 18,
                    top + 248,
                    ROW_W,
                    noticeError ? 0xFFFF7070 : 0xFF80FF80
            );
        }
            super.render(graphics, mouseX, mouseY, partialTick);
}

    private void drawFeatureRow(GuiGraphics graphics,
                                int mouseX, int mouseY,
                                int x, int y,
                                String title,
                                String description,
                                boolean enabled) {
        boolean hovered = hit(mouseX, mouseY, x, y, ROW_W, ROW_H);
        graphics.fill(x, y, x + ROW_W, y + ROW_H, hovered ? 0xFF222222 : 0xFF1D1D1D);
        GuiCompat.outline(graphics, x, y, ROW_W, ROW_H, hovered ? 0xFF777777 : 0xFF444444);

        graphics.drawString(this.font, Component.literal(title), x + 12, y + 10, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.literal(description), x + 12, y + 27, 0xFF9A9A9A, false);

        int toggleX = x + ROW_W - TOGGLE_W - 10;
        int toggleY = y + 13;
        int fill = enabled ? 0xFF315E3B : 0xFF5C3030;
        int border = enabled ? 0xFF76D486 : 0xFFD47A7A;
        int text = enabled ? 0xFFB8FFC2 : 0xFFFFBDBD;
        graphics.fill(toggleX, toggleY, toggleX + TOGGLE_W, toggleY + TOGGLE_H, fill);
        GuiCompat.outline(graphics, toggleX, toggleY, TOGGLE_W, TOGGLE_H, border);
        graphics.drawCenteredString(this.font,
                Component.literal(enabled ? "ENABLED" : "DISABLED"),
                toggleX + TOGGLE_W / 2,
                toggleY + 7,
                text);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) {
            return super.mouseClicked(mouseX, mouseY, button);
        }

        double mx = mouseX;
        double my = mouseY;
        int left = panelLeft();
        int top = panelTop();
        int x = left + ROW_X_OFFSET;

        if (hit(mx, my, x, top + 55, ROW_W, ROW_H)) {
            sendToggle("wooden_tools", !woodenTools);
            return true;
        }
        if (hit(mx, my, x, top + 111, ROW_W, ROW_H)) {
            sendToggle("stone_tools", !stoneTools);
            return true;
        }
        if (hit(mx, my, x, top + 167, ROW_W, ROW_H)) {
            sendToggle("tree_punching", !treePunching);
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendToggle(String key, boolean enabled) {
        JsonObject root = new JsonObject();
        root.addProperty("action", "set_feature");
        root.addProperty("key", key);
        root.addProperty("enabled", enabled);
        PacketDistributor.sendToServer(SettingsPayload.action(root.toString()));
    }

    private int panelLeft() {
        return Math.max(6, (this.width - Math.min(PANEL_W, this.width - 12)) / 2);
    }

    private int panelTop() {
        return Math.max(6, (this.height - Math.min(PANEL_H, this.height - 12)) / 2);
    }

    private static boolean hit(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
