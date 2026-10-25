package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.client.gui.KnappingGuiTheme;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Fully code-drawn GUI, with only the actual knapping material using game textures. */
public class KnappingScreen extends AbstractContainerScreen<KnappingMenu> {
    private static final int TILE_SIZE = 12;
    private static final int GRID_X = 13;
    private static final int GRID_Y = 26;
    private static final int CONTROL_Y = 80;
    private Identifier cachedTexture;
    private boolean showRecipeHint = true;
    private int lastHit = -1;
    private int flashTicks;

    public KnappingScreen(KnappingMenu menu, Inventory inventory, Component title) {
        // Show the actual input item, not the knapping type's internal name.
        // Reuses the existing translated "Material" label (no language-file change needed).
        super(menu, inventory, Component.translatable("gui.simpleknapping.material")
                .append(": ")
                .append(new ItemStack(menu.getInputMaterial()).getHoverName()), 176, 194);
        titleLabelX = 9;
        titleLabelY = 5;
        inventoryLabelX = 9;
        inventoryLabelY = 94;
    }

    @Override
    protected void init() {
        super.init();
        cachedTexture = chooseTileTexture();
    }

    // AbstractContainerScreen#tick() is final in Minecraft 26.2.
    // Use the dedicated per-tick extension point instead.
    @Override
    protected void containerTick() {
        super.containerTick();
        if (flashTicks > 0) flashTicks--;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, KnappingGuiTheme.BACKDROP);
        KnappingGuiTheme.panel(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 3, topPos + 16, leftPos + 173, topPos + 17, 0xFF484848);
        KnappingGuiTheme.surface(graphics, leftPos + 10, topPos + 23, 66, 66);
        KnappingGuiTheme.surface(graphics, leftPos + 124, topPos + 42, 24, 24);
        graphics.text(font, Component.translatable("gui.simpleknapping.output"),
                leftPos + 122, topPos + 33, KnappingGuiTheme.MUTED, false);

        drawKnappingTiles(graphics);
        drawPlayerInventoryBackground(graphics);
        int infoY = topPos + 52;
        if (menu.canViewRecipePreview()) {
            graphics.text(font, Component.translatable("gui.simpleknapping.cost", menu.previewCost()),
                    leftPos + 88, infoY, KnappingGuiTheme.MUTED, false);
            drawButton(graphics, mouseX, mouseY, 88, 66, 80, 11,
                    showRecipeHint ? "gui.simpleknapping.hints_on" : "gui.simpleknapping.hints_off", showRecipeHint);
            drawButton(graphics, mouseX, mouseY, 88, CONTROL_Y, 20, 12, "gui.simpleknapping.previous", false);
            drawButton(graphics, mouseX, mouseY, 148, CONTROL_Y, 20, 12, "gui.simpleknapping.next", false);
            graphics.centeredText(font, Component.translatable("gui.simpleknapping.recipe_count",
                            menu.previewCount() == 0 ? 0 : menu.previewIndex() + 1, menu.previewCount()),
                    leftPos + 104, topPos + 40, KnappingGuiTheme.MUTED);
        }
        // Reset is visible only to Creative players, independently of the hint config.
        if (isCreativePlayer()) {
            drawButton(graphics, mouseX, mouseY, 110, CONTROL_Y, 36, 12, "gui.simpleknapping.reset", false);
        }
    }

    private void drawButton(GuiGraphicsExtractor graphics, int mx, int my,
                            int x, int y, int w, int h, String key, boolean active) {
        int sx = leftPos + x;
        int sy = topPos + y;
        boolean hovered = KnappingGuiTheme.hit(mx, my, sx, sy, w, h);
        KnappingGuiTheme.button(graphics, sx, sy, w, h, hovered, active);
        graphics.centeredText(font, Component.translatable(key), sx + w / 2,
                sy + Math.max(1, (h - 8) / 2), active ? KnappingGuiTheme.ACCENT : KnappingGuiTheme.TEXT);
    }

    /** Inventory cells were previously drawn in the removed GUI PNG. */
    private void drawPlayerInventoryBackground(GuiGraphicsExtractor graphics) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                drawInventoryCell(graphics, leftPos + 7 + column * 18, topPos + 103 + row * 18);
            }
        }
        for (int column = 0; column < 9; column++) {
            drawInventoryCell(graphics, leftPos + 7 + column * 18, topPos + 161);
        }
    }

    private void drawInventoryCell(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x, y, x + 18, y + 18, 0xFF101010);
        graphics.outline(x, y, 18, 18, 0xFF5B5B5B);
    }

    private void drawKnappingTiles(GuiGraphicsExtractor graphics) {
        Identifier texture = cachedTexture == null ? KnappingTypeManager.DEFAULT_TEXTURE : cachedTexture;
        for (int i = 0; i < KnappingMenu.TILE_COUNT; i++) {
            int x = leftPos + GRID_X + (i % KnappingMenu.GRID_SIZE) * TILE_SIZE;
            int y = topPos + GRID_Y + (i / KnappingMenu.GRID_SIZE) * TILE_SIZE;
            graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
                    TILE_SIZE - 1, TILE_SIZE - 1, 16, 16);
            if (!menu.hasTile(i)) graphics.fill(x, y, x + TILE_SIZE - 1, y + TILE_SIZE - 1, 0xB5000000);
            if (menu.canViewRecipePreview() && showRecipeHint && menu.previewCount() > 0
                    && !menu.hasPreviewTile(i)) {
                graphics.outline(x, y, TILE_SIZE - 1, TILE_SIZE - 1, 0xFF8EC9EF);
            }
            if (flashTicks > 0 && lastHit == i) {
                int opacity = 0x20 + flashTicks * 12;
                graphics.fill(x, y, x + TILE_SIZE - 1, y + TILE_SIZE - 1,
                        (Math.min(255, opacity) << 24) | 0x00FFFFFF);
            }
        }
    }

    private Identifier chooseTileTexture() {
        Item input = menu.getInputMaterial();
        if (input instanceof BlockItem blockItem) {
            Identifier block = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (block != null) {
                Identifier inputTexture = KnappingTypeManager.textureForBlock(block);
                if (available(inputTexture)) return inputTexture;
            }
        }
        if (menu.getKnappingType() != null && available(menu.getKnappingType().texture())) {
            return menu.getKnappingType().texture();
        }
        return KnappingTypeManager.DEFAULT_TEXTURE;
    }

    private boolean available(Identifier id) {
        try {
            return id != null && Minecraft.getInstance().getResourceManager().getResource(id).isPresent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x();
        double my = event.y();
        if (menu.canViewRecipePreview()) {
            if (KnappingGuiTheme.hit(mx, my, leftPos + 88, topPos + 66, 80, 11)) {
                showRecipeHint = !showRecipeHint;
                clickSound();
                return true;
            }
            if (KnappingGuiTheme.hit(mx, my, leftPos + 88, topPos + CONTROL_Y, 20, 12)) {
                sendButton(KnappingMenu.PREVIOUS_BUTTON);
                return true;
            }
            if (KnappingGuiTheme.hit(mx, my, leftPos + 148, topPos + CONTROL_Y, 20, 12)) {
                sendButton(KnappingMenu.NEXT_BUTTON);
                return true;
            }
        }
        if (isCreativePlayer()
                && KnappingGuiTheme.hit(mx, my, leftPos + 110, topPos + CONTROL_Y, 36, 12)) {
            sendButton(KnappingMenu.RESET_BUTTON);
            return true;
        }
        // Use bounds checks on actual screen coordinates BEFORE integer division: Java truncates
        // negative fractions toward zero, which could otherwise register clicks outside the grid.
        double gridLeft = leftPos + GRID_X;
        double gridTop = topPos + GRID_Y;
        if (mx >= gridLeft && my >= gridTop && mx < gridLeft + KnappingMenu.GRID_SIZE * TILE_SIZE
                && my < gridTop + KnappingMenu.GRID_SIZE * TILE_SIZE) {
            int x = (int) ((mx - gridLeft) / TILE_SIZE);
            int y = (int) ((my - gridTop) / TILE_SIZE);
            int index = y * KnappingMenu.GRID_SIZE + x;
            if (menu.hasTile(index)) {
                lastHit = index;
                flashTicks = 6;
                sendButton(index);
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private boolean isCreativePlayer() {
        return minecraft != null && minecraft.player != null && minecraft.player.isCreative();
    }

    private void sendButton(int button) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, button);
            clickSound();
        }
    }

    private void clickSound() {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 0.8F));
        }
    }
}
