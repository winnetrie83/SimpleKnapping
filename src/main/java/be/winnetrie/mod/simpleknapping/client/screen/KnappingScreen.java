package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

public class KnappingScreen extends AbstractContainerScreen<KnappingMenu> {

    private static final int TILE_SIZE = 12;
    private static final int GRID_X = 13;
    private static final int GRID_Y = 18;

    private static final Identifier BACKGROUND =
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "textures/gui/knapping.png");

    public KnappingScreen(KnappingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 194);
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 89;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                BACKGROUND,
                this.leftPos,
                this.topPos,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                256,
                256
        );
        drawKnappingTiles(guiGraphics);
    }

    /**
     * Texture priority for the carving/knapping surface:
     * 1) the recipe material itself when that material is a block item;
     * 2) the custom fallback texture configured on the knapping type;
     * 3) the mod's guaranteed minecraft:clay fallback.
     *
     * This means recipes using oak_log, dark_oak_log, modded block items, etc.
     * automatically look like the material being carved without requiring a
     * separate texture override for every recipe.
     */
    private Identifier getTileTexture() {
        Identifier materialTexture = textureFromInputMaterial();
        if (isAvailableTexture(materialTexture)) {
            return materialTexture;
        }

        if (this.menu.getKnappingType() != null) {
            Identifier configuredFallback = this.menu.getKnappingType().texture();
            if (isAvailableTexture(configuredFallback)) {
                return configuredFallback;
            }
        }

        return KnappingTypeManager.DEFAULT_TEXTURE;
    }

    private Identifier textureFromInputMaterial() {
        Item inputMaterial = this.menu.getInputMaterial();
        if (!(inputMaterial instanceof BlockItem blockItem)) {
            return null;
        }

        Identifier blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        if (blockId == null) {
            return null;
        }
        return KnappingTypeManager.textureForBlock(blockId);
    }

    private boolean isAvailableTexture(Identifier texture) {
        if (texture == null) {
            return false;
        }

        try {
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void drawKnappingTiles(GuiGraphicsExtractor guiGraphics) {
        Identifier tileTexture = getTileTexture();
        for (int y = 0; y < KnappingMenu.GRID_SIZE; y++) {
            for (int x = 0; x < KnappingMenu.GRID_SIZE; x++) {
                int index = y * KnappingMenu.GRID_SIZE + x;
                int tileX = this.leftPos + GRID_X + x * TILE_SIZE;
                int tileY = this.topPos + GRID_Y + y * TILE_SIZE;
                drawTileDark(guiGraphics, tileTexture, tileX, tileY);
                if (this.menu.hasTile(index)) {
                    drawTileNormal(guiGraphics, tileTexture, tileX, tileY);
                }
            }
        }
    }

    private void drawTileDark(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, TILE_SIZE, TILE_SIZE, 16, 16);
        guiGraphics.fill(x, y, x + TILE_SIZE, y + TILE_SIZE, 0x88000000);
    }

    private void drawTileNormal(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F, TILE_SIZE, TILE_SIZE, 16, 16);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mouseX = event.x();
            double mouseY = event.y();
            int gridLeft = this.leftPos + GRID_X;
            int gridTop = this.topPos + GRID_Y;
            int x = (int) ((mouseX - gridLeft) / TILE_SIZE);
            int y = (int) ((mouseY - gridTop) / TILE_SIZE);

            if (x >= 0 && x < KnappingMenu.GRID_SIZE && y >= 0 && y < KnappingMenu.GRID_SIZE) {
                int index = y * KnappingMenu.GRID_SIZE + x;
                if (this.menu.hasTile(index)) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
