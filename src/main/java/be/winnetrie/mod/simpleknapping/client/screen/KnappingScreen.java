package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

@SuppressWarnings("null")
public class KnappingScreen extends AbstractContainerScreen<KnappingMenu> {

    private static final int TILE_SIZE = 12;
    private static final int GRID_X = 13;
    private static final int GRID_Y = 18;

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "textures/gui/knapping.png");

    public KnappingScreen(KnappingMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 194;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = 89;
    }

    /**
     * Minecraft 1.21.1 uses a different container-screen render order than
     * the modern 26.2 GUI extractor path.  Rendering the custom panel only
     * from renderBg can leave the slot contents and labels visible while the
     * actual knapping panel/grid disappears.
     *
     * Draw the textured panel here instead. This method is called before the
     * container foreground (slots/labels), so the panel and 5x5 knapping
     * surface stay behind the interactive contents without re-enabling the
     * vanilla blur. The GUI texture contains intentional transparent pixels,
     * so no opaque fallback rectangle is drawn behind it.
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Keep the world readable behind the GUI, but do not use vanilla blur.
        graphics.fill(0, 0, this.width, this.height, 0x80000000);

        graphics.blit(
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
        drawKnappingTiles(graphics);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        // Intentionally empty on 1.21.1.  The complete panel is rendered from
        // renderBackground above so it is not lost by the older container
        // render ordering.
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
    private ResourceLocation getTileTexture() {
        ResourceLocation materialTexture = textureFromInputMaterial();
        if (isAvailableTexture(materialTexture)) {
            return materialTexture;
        }

        if (this.menu.getKnappingType() != null) {
            ResourceLocation configuredFallback = this.menu.getKnappingType().texture();
            if (isAvailableTexture(configuredFallback)) {
                return configuredFallback;
            }
        }

        return KnappingTypeManager.DEFAULT_TEXTURE;
    }

    private ResourceLocation textureFromInputMaterial() {
        Item inputMaterial = this.menu.getInputMaterial();
        if (!(inputMaterial instanceof BlockItem blockItem)) {
            return null;
        }

        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
        return KnappingTypeManager.textureForBlock(blockId);
    }

    private boolean isAvailableTexture(ResourceLocation texture) {
        if (texture == null) {
            return false;
        }

        try {
            return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent();
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void drawKnappingTiles(GuiGraphics guiGraphics) {
        ResourceLocation tileTexture = getTileTexture();
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

    private void drawTileDark(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 0.0F, 0.0F, TILE_SIZE, TILE_SIZE, 16, 16);
        guiGraphics.fill(x, y, x + TILE_SIZE, y + TILE_SIZE, 0x88000000);
    }

    private void drawTileNormal(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y) {
        guiGraphics.blit(texture, x, y, 0.0F, 0.0F, TILE_SIZE, TILE_SIZE, 16, 16);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int gridLeft = this.leftPos + GRID_X;
            int gridTop = this.topPos + GRID_Y;
            int x = (int) ((mouseX - gridLeft) / TILE_SIZE);
            int y = (int) ((mouseY - gridTop) / TILE_SIZE);

            if (x >= 0 && x < KnappingMenu.GRID_SIZE && y >= 0 && y < KnappingMenu.GRID_SIZE) {
                int index = y * KnappingMenu.GRID_SIZE + x;
                if (this.menu.hasTile(index) && this.minecraft != null && this.minecraft.gameMode != null) {
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
