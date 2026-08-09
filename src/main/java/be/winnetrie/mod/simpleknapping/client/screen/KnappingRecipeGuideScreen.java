package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.guide.RecipeGuideSnapshot;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Public read-only browser for every currently usable Simple Knapping recipe.
 * Opened with /simpleknapping guide; no OP/admin permission is required.
 */
public final class KnappingRecipeGuideScreen extends Screen {
    private static final int PANEL_W = 580;
    private static final int PANEL_H = 320;
    private static final int LIST_W = 190;
    private static final int ROW_H = 20;
    private static final int RECIPE_ROWS = 10;
    private static final int PATTERN_TILE = 26;

    private final RecipeGuideSnapshot snapshot;
    private EditBox search;
    private String selectedId;
    private int page;

    public KnappingRecipeGuideScreen(RecipeGuideSnapshot snapshot) {
        super(Component.literal("Knapping Recipe Guide"));
        this.snapshot = snapshot;
        if (!snapshot.recipes().isEmpty()) {
            this.selectedId = snapshot.recipes().get(0).id();
        }
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        search = new EditBox(this.font, left + 8, top + 30, LIST_W - 16, 18, Component.literal("Search recipes"));
        search.setMaxLength(100);
        addRenderableWidget(search);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int right = left + Math.min(PANEL_W, this.width - 12);
        int bottom = top + Math.min(PANEL_H, this.height - 12);
        int detailsX = left + LIST_W + 14;

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        graphics.outline(left, top, right - left, bottom - top, 0xFF6A6A6A);

        graphics.text(this.font, Component.literal("Knapping Recipe Guide"), left + 8, top + 8, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("All active Simple Knapping recipes"), left + 8, top + 19, 0xFF9E9E9E, false);
        graphics.fill(left + LIST_W, top + 7, left + LIST_W + 1, bottom - 7, 0xFF454545);

        drawRecipeList(graphics, mouseX, mouseY, left, top);
        RecipeGuideSnapshot.Entry entry = selectedEntry();
        if (entry == null) {
            graphics.centeredText(
                    this.font,
                    Component.literal("No active knapping recipes found."),
                    detailsX + (right - detailsX) / 2,
                    top + 150,
                    0xFFBDBDBD
            );
        } else {
            drawRecipeDetails(graphics, mouseX, mouseY, detailsX, top, entry);
        }
    }

    private void drawRecipeList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        List<RecipeGuideSnapshot.Entry> recipes = filteredRecipes();
        int maxPage = Math.max(0, (recipes.size() - 1) / RECIPE_ROWS);
        page = Math.min(page, maxPage);

        int start = page * RECIPE_ROWS;
        int rowY = top + 55;
        for (int i = 0; i < RECIPE_ROWS && start + i < recipes.size(); i++) {
            RecipeGuideSnapshot.Entry entry = recipes.get(start + i);
            int y = rowY + i * ROW_H;
            boolean selected = entry.id().equals(selectedId);
            boolean hovered = hit(mouseX, mouseY, left + 7, y, LIST_W - 14, ROW_H - 1);

            if (selected) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF303030);
            }

            ItemStack result = stackFor(entry.resultItem());
            if (!result.isEmpty()) {
                graphics.item(result, left + 9, y + 1);
                if (hovered) {
                    graphics.setTooltipForNextFrame(this.font, result, mouseX, mouseY);
                }
            }
            graphics.text(this.font,
                    Component.literal(trim(itemName(result, entry.resultItem()), 22)),
                    left + 29,
                    y + 5,
                    0xFFE5E5E5,
                    false);
        }

        drawButton(graphics, left + 8, top + 266, 30, 18, "<", page > 0, mouseX, mouseY);
        graphics.centeredText(this.font,
                Component.literal((page + 1) + "/" + (maxPage + 1)),
                left + LIST_W / 2,
                top + 271,
                0xFFBDBDBD);
        drawButton(graphics, left + LIST_W - 38, top + 266, 30, 18, ">", page < maxPage, mouseX, mouseY);
        graphics.text(this.font,
                Component.literal(recipes.size() + (recipes.size() == 1 ? " recipe" : " recipes")),
                left + 8, top + 294, 0xFF777777, false);
    }

    private void drawRecipeDetails(GuiGraphicsExtractor graphics,
                                   int mouseX,
                                   int mouseY,
                                   int x,
                                   int top,
                                   RecipeGuideSnapshot.Entry entry) {
        ItemStack result = stackFor(entry.resultItem());
        ItemStack tool = stackFor(entry.tool());
        ItemStack material = stackFor(entry.material());

        graphics.text(this.font, Component.literal("Recipe"), x, top + 14, 0xFFBDBDBD, false);
        graphics.fill(x, top + 29, x + 36, top + 65, 0xFF292929);
        graphics.outline(x, top + 29, 36, 36, 0xFF777777);
        if (!result.isEmpty()) {
            graphics.item(result, x + 10, top + 39);
            graphics.itemDecorations(this.font, result, x + 10, top + 39);
            if (hit(mouseX, mouseY, x, top + 29, 36, 36)) {
                graphics.setTooltipForNextFrame(this.font, result, mouseX, mouseY);
            }
        }

        String resultName = itemName(result, entry.resultItem());
        graphics.text(this.font, Component.literal(trim(resultName, 37)), x + 46, top + 31, 0xFFFFFFFF, false);
        graphics.text(this.font,
                Component.literal("Result: " + entry.resultCount() + " x " + trim(resultName, 29)),
                x + 46, top + 45, 0xFFBDBDBD, false);
        graphics.text(this.font,
                Component.literal("Type: " + displayId(entry.knappingType())),
                x + 46, top + 58, 0xFF9AC7FF, false);
        graphics.text(this.font,
                Component.literal(trim(entry.id(), 52)),
                x, top + 77, 0xFF6F6F6F, false);

        graphics.text(this.font,
                Component.literal("Target 5 x 5 pattern"),
                x, top + 96, 0xFFBDBDBD, false);
        int gridX = x;
        int gridY = top + 111;
        Identifier texture = patternTexture(entry, material);
        for (int row = 0; row < 5; row++) {
            String patternRow = row < entry.pattern().size() ? entry.pattern().get(row) : "";
            for (int col = 0; col < 5; col++) {
                int tx = gridX + col * PATTERN_TILE;
                int ty = gridY + row * PATTERN_TILE;
                boolean remains = col < patternRow.length() && patternRow.charAt(col) == 'X';
                graphics.blit(
                        RenderPipelines.GUI_TEXTURED,
                        texture,
                        tx,
                        ty,
                        0.0F,
                        0.0F,
                        PATTERN_TILE - 2,
                        PATTERN_TILE - 2,
                        16,
                        16
                );
                if (!remains) {
                    graphics.fill(tx, ty, tx + PATTERN_TILE - 2, ty + PATTERN_TILE - 2, 0xA8000000);
                }
                graphics.outline(tx, ty, PATTERN_TILE - 2, PATTERN_TILE - 2,
                        remains ? 0xFFBDBDBD : 0xFF4B4B4B);
            }
        }

        int infoX = x + 154;
        drawItemInfoRow(graphics, mouseX, mouseY, infoX, top + 108,
                "Knapping tool", tool, itemName(tool, entry.tool()), "");
        drawItemInfoRow(graphics, mouseX, mouseY, infoX, top + 160,
                "Material", material, itemName(material, entry.material()), "x " + entry.materialCost());
        drawItemInfoRow(graphics, mouseX, mouseY, infoX, top + 212,
                "Result", result, itemName(result, entry.resultItem()), "x " + entry.resultCount());

    }

    private void drawItemInfoRow(GuiGraphicsExtractor graphics,
                                 int mouseX,
                                 int mouseY,
                                 int x,
                                 int y,
                                 String label,
                                 ItemStack stack,
                                 String name,
                                 String amount) {
        graphics.text(this.font, Component.literal(label), x, y, 0xFFBDBDBD, false);
        graphics.fill(x, y + 12, x + 34, y + 46, 0xFF292929);
        graphics.outline(x, y + 12, 34, 34, 0xFF666666);
        if (!stack.isEmpty()) {
            graphics.item(stack, x + 9, y + 21);
            if (hit(mouseX, mouseY, x, y + 12, 34, 34)) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }
        graphics.text(this.font, Component.literal(trim(name, 25)), x + 43, y + 18, 0xFFE5E5E5, false);
        if (!amount.isBlank()) {
            graphics.text(this.font, Component.literal(amount), x + 43, y + 32, 0xFF9AD59A, false);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }

        double mx = event.x();
        double my = event.y();
        int left = panelLeft();
        int top = panelTop();
        List<RecipeGuideSnapshot.Entry> recipes = filteredRecipes();
        int start = page * RECIPE_ROWS;
        int rowY = top + 55;

        for (int i = 0; i < RECIPE_ROWS && start + i < recipes.size(); i++) {
            int y = rowY + i * ROW_H;
            if (hit(mx, my, left + 7, y, LIST_W - 14, ROW_H - 1)) {
                selectedId = recipes.get(start + i).id();
                return true;
            }
        }

        int maxPage = Math.max(0, (recipes.size() - 1) / RECIPE_ROWS);
        if (hit(mx, my, left + 8, top + 266, 30, 18) && page > 0) {
            page--;
            return true;
        }
        if (hit(mx, my, left + LIST_W - 38, top + 266, 30, 18) && page < maxPage) {
            page++;
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private List<RecipeGuideSnapshot.Entry> filteredRecipes() {
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<RecipeGuideSnapshot.Entry> entries = new ArrayList<>(snapshot.recipes());
        entries.sort(Comparator.comparing(entry -> itemName(stackFor(entry.resultItem()), entry.resultItem()).toLowerCase(Locale.ROOT)));
        if (query.isBlank()) {
            return entries;
        }

        return entries.stream()
                .filter(entry -> {
                    String resultName = itemName(stackFor(entry.resultItem()), entry.resultItem()).toLowerCase(Locale.ROOT);
                    String materialName = itemName(stackFor(entry.material()), entry.material()).toLowerCase(Locale.ROOT);
                    String toolName = itemName(stackFor(entry.tool()), entry.tool()).toLowerCase(Locale.ROOT);
                    return resultName.contains(query)
                            || materialName.contains(query)
                            || toolName.contains(query)
                            || entry.id().toLowerCase(Locale.ROOT).contains(query)
                            || entry.knappingType().toLowerCase(Locale.ROOT).contains(query)
                            || entry.resultItem().toLowerCase(Locale.ROOT).contains(query)
                            || entry.material().toLowerCase(Locale.ROOT).contains(query);
                })
                .toList();
    }

    private RecipeGuideSnapshot.Entry selectedEntry() {
        if (selectedId != null) {
            for (RecipeGuideSnapshot.Entry entry : snapshot.recipes()) {
                if (entry.id().equals(selectedId)) {
                    return entry;
                }
            }
        }
        return snapshot.recipes().isEmpty() ? null : snapshot.recipes().get(0);
    }

    private Identifier patternTexture(RecipeGuideSnapshot.Entry entry, ItemStack materialStack) {
        if (!materialStack.isEmpty() && materialStack.getItem() instanceof BlockItem blockItem) {
            Identifier blockId = BuiltInRegistries.BLOCK.getKey(blockItem.getBlock());
            if (blockId != null) {
                Identifier materialTexture = KnappingTypeManager.textureForBlock(blockId);
                if (isAvailableTexture(materialTexture)) {
                    return materialTexture;
                }
            }
        }

        try {
            Identifier fallback = Identifier.parse(entry.fallbackTexture());
            if (isAvailableTexture(fallback)) {
                return fallback;
            }
        } catch (RuntimeException ignored) {
        }
        return KnappingTypeManager.DEFAULT_TEXTURE;
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

    private static ItemStack stackFor(String id) {
        try {
            Identifier identifier = Identifier.parse(id);
            if (!BuiltInRegistries.ITEM.containsKey(identifier)) {
                return ItemStack.EMPTY;
            }
            Item item = BuiltInRegistries.ITEM.getValue(identifier);
            return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
        } catch (RuntimeException ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String itemName(ItemStack stack, String fallback) {
        return stack.isEmpty() ? fallback : stack.getHoverName().getString();
    }

    private static String displayId(String id) {
        String path = id;
        try {
            path = Identifier.parse(id).getPath();
        } catch (RuntimeException ignored) {
        }
        String[] words = path.split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (!result.isEmpty()) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                result.append(word.substring(1));
            }
        }
        return result.isEmpty() ? id : result.toString();
    }

    private static void drawButton(GuiGraphicsExtractor graphics,
                                   int x,
                                   int y,
                                   int width,
                                   int height,
                                   String label,
                                   boolean enabled,
                                   int mouseX,
                                   int mouseY) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, width, height);
        int fill = !enabled ? 0xFF252525 : hovered ? 0xFF4A4A4A : 0xFF353535;
        int border = !enabled ? 0xFF444444 : hovered ? 0xFFBDBDBD : 0xFF747474;
        int text = enabled ? 0xFFFFFFFF : 0xFF666666;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
        graphics.centeredText(Minecraft.getInstance().font, Component.literal(label), x + width / 2, y + 5, text);
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

    private static String trim(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 3)) + "...";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
