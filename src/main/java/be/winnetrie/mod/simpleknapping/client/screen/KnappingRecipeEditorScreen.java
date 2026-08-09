package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.admin.RecipeEditorSnapshot;
import be.winnetrie.mod.simpleknapping.network.RecipeEditorPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Admin/OP-facing recipe manager. This is intentionally a normal client screen,
 * not a container screen: all authority remains on the server and every write
 * is validated again by RecipeEditorNetwork.
 */
public final class KnappingRecipeEditorScreen extends Screen {
    private static final int PANEL_W = 720;
    private static final int PANEL_H = 390;
    private static final int LIST_W = 205;
    private static final int ROW_H = 18;
    private static final int RECIPE_ROWS = 13;
    private static final int TILE = 22;
    private static final int ITEM_COLS = 8;
    private static final int ITEM_ROWS = 4;
    private static final int ITEMS_PER_PAGE = ITEM_COLS * ITEM_ROWS;

    private RecipeEditorSnapshot snapshot;
    private String selectedRecipeId;

    private EditBox recipeSearch;
    private EditBox recipeId;
    private EditBox resultSearch;
    private EditBox resultCount;

    private final boolean[] pattern = new boolean[25];
    private String selectedType = "";
    private Identifier selectedResult = Identifier.withDefaultNamespace("flint");

    private int recipePage;
    private int itemPage;
    private boolean creatingNew;

    public KnappingRecipeEditorScreen(RecipeEditorSnapshot snapshot) {
        super(Component.literal("Knapping Recipe Manager"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int editorX = left + LIST_W + 18;

        recipeSearch = new EditBox(this.font, left + 10, top + 31, LIST_W - 20, 18, Component.literal("Recipe search"));
        recipeSearch.setMaxLength(100);
        addRenderableWidget(recipeSearch);

        recipeId = new EditBox(this.font, editorX, top + 49, 286, 18, Component.literal("Recipe id"));
        recipeId.setMaxLength(160);
        addRenderableWidget(recipeId);

        resultSearch = new EditBox(this.font, editorX + 139, top + 158, 190, 18, Component.literal("Item search"));
        resultSearch.setMaxLength(100);
        addRenderableWidget(resultSearch);

        resultCount = new EditBox(this.font, editorX + 363, top + 49, 44, 18, Component.literal("Count"));
        resultCount.setMaxLength(2);
        addRenderableWidget(resultCount);

        if (selectedRecipeId != null) {
            selectRecipeById(selectedRecipeId);
        } else if (!snapshot.recipes().isEmpty()) {
            selectRecipe(snapshot.recipes().get(0));
        } else {
            beginNewRecipe();
        }
    }

    public void applyServerSnapshot(RecipeEditorSnapshot newSnapshot) {
        String keepId = recipeId != null ? recipeId.getValue().trim() : selectedRecipeId;
        this.snapshot = newSnapshot;
        this.recipePage = 0;

        if (keepId != null && selectRecipeById(keepId)) {
            return;
        }

        if (!newSnapshot.recipes().isEmpty()) {
            selectRecipe(newSnapshot.recipes().get(0));
        } else {
            beginNewRecipe();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int right = left + Math.min(PANEL_W, this.width - 12);
        int bottom = top + Math.min(PANEL_H, this.height - 12);
        int editorX = left + LIST_W + 18;

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        graphics.outline(left, top, right - left, bottom - top, 0xFF6A6A6A);

        graphics.text(this.font, Component.literal("Knapping Recipe Manager"), left + 10, top + 10, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("Recipes"), left + 10, top + 21, 0xFFBDBDBD, false);
        graphics.fill(left + LIST_W, top + 8, left + LIST_W + 1, bottom - 8, 0xFF454545);

        drawRecipeList(graphics, mouseX, mouseY, left, top);
        drawEditor(graphics, mouseX, mouseY, editorX, top);

        if (!snapshot.notice().isBlank()) {
            int noticeColor = snapshot.noticeError() ? 0xFFFF7070 : 0xFF80FF80;
            graphics.textWithWordWrap(
                    this.font,
                    Component.literal(snapshot.notice()),
                    editorX,
                    bottom - 35,
                    Math.max(120, right - editorX - 10),
                    noticeColor
            );
        }
    }

    private void drawRecipeList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        List<RecipeEditorSnapshot.RecipeEntry> filtered = filteredRecipes();
        int maxPage = Math.max(0, (filtered.size() - 1) / RECIPE_ROWS);
        recipePage = Math.min(recipePage, maxPage);

        int start = recipePage * RECIPE_ROWS;
        int rowY = top + 55;
        for (int i = 0; i < RECIPE_ROWS && start + i < filtered.size(); i++) {
            RecipeEditorSnapshot.RecipeEntry entry = filtered.get(start + i);
            int y = rowY + i * ROW_H;
            boolean selected = entry.id().equals(selectedRecipeId) && !creatingNew;
            boolean hovered = hit(mouseX, mouseY, left + 8, y, LIST_W - 16, ROW_H - 1);

            if (selected) {
                graphics.fill(left + 8, y, left + LIST_W - 8, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(left + 8, y, left + LIST_W - 8, y + ROW_H - 1, 0xFF303030);
            }

            int color = entry.disabled() ? 0xFF888888 : 0xFFE5E5E5;
            graphics.text(this.font, trim(entry.id(), 28), left + 12, y + 5, color, false);

            String badge = switch (entry.origin()) {
                case "CUSTOM" -> "C";
                case "OVERRIDE" -> "O";
                default -> "R";
            };
            graphics.text(this.font, badge, left + LIST_W - 19, y + 5,
                    "RESOURCE".equals(entry.origin()) ? 0xFFB0B0B0 : 0xFFFFD66B, false);
        }

        drawButton(graphics, left + 8, top + 298, 90, 20, "New", true, mouseX, mouseY);
        drawButton(graphics, left + 105, top + 298, 90, 20, "Duplicate", selectedEntry() != null, mouseX, mouseY);
        drawButton(graphics, left + 8, top + 324, 28, 18, "<", recipePage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((recipePage + 1) + "/" + (maxPage + 1)), left + 82, top + 329, 0xFFBDBDBD, false);
        drawButton(graphics, left + 167, top + 324, 28, 18, ">", recipePage < maxPage, mouseX, mouseY);

        graphics.text(this.font, Component.literal("R resource   C custom   O override"), left + 9, top + 350, 0xFF777777, false);
    }

    private void drawEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int top) {
        graphics.text(this.font, Component.literal(creatingNew ? "New recipe" : "Edit recipe"), x, top + 21, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("Recipe ID"), x, top + 38, 0xFFBDBDBD, false);
        graphics.text(this.font, Component.literal("Count"), x + 363, top + 38, 0xFFBDBDBD, false);

        graphics.text(this.font, Component.literal("Knapping type"), x, top + 77, 0xFFBDBDBD, false);
        drawButton(graphics, x, top + 88, 180, 20, selectedType.isBlank() ? "No types loaded" : selectedType,
                !snapshot.knappingTypes().isEmpty(), mouseX, mouseY);

        graphics.text(this.font, Component.literal("5 x 5 pattern"), x, top + 119, 0xFFBDBDBD, false);
        int gridX = x;
        int gridY = top + 135;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int index = row * 5 + col;
                int tileX = gridX + col * TILE;
                int tileY = gridY + row * TILE;
                boolean hover = hit(mouseX, mouseY, tileX, tileY, TILE - 2, TILE - 2);
                int fill = pattern[index] ? 0xFFC8C8C8 : 0xFF303030;
                if (hover) {
                    fill = pattern[index] ? 0xFFE8E8E8 : 0xFF464646;
                }
                graphics.fill(tileX, tileY, tileX + TILE - 2, tileY + TILE - 2, fill);
                graphics.outline(tileX, tileY, TILE - 2, TILE - 2, 0xFF666666);
            }
        }

        graphics.text(this.font, Component.literal("Result item"), x + 139, top + 119, 0xFFBDBDBD, false);
        ItemStack selectedStack = stackFor(selectedResult);
        graphics.fill(x + 347, top + 129, x + 379, top + 161, 0xFF292929);
        graphics.outline(x + 347, top + 129, 32, 32, 0xFF777777);
        if (!selectedStack.isEmpty()) {
            graphics.item(selectedStack, x + 355, top + 137);
            graphics.itemDecorations(this.font, selectedStack, x + 355, top + 137);
            if (hit(mouseX, mouseY, x + 347, top + 129, 32, 32)) {
                graphics.setTooltipForNextFrame(this.font, selectedStack, mouseX, mouseY);
            }
        }

        List<Item> items = filteredItems();
        int maxItemPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        itemPage = Math.min(itemPage, maxItemPage);
        int itemStart = itemPage * ITEMS_PER_PAGE;
        int pickerX = x + 139;
        int pickerY = top + 184;

        for (int i = 0; i < ITEMS_PER_PAGE && itemStart + i < items.size(); i++) {
            Item item = items.get(itemStart + i);
            ItemStack stack = new ItemStack(item);
            int col = i % ITEM_COLS;
            int row = i / ITEM_COLS;
            int ix = pickerX + col * TILE;
            int iy = pickerY + row * TILE;
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            boolean selected = itemId.equals(selectedResult);
            boolean hover = hit(mouseX, mouseY, ix, iy, TILE - 2, TILE - 2);

            graphics.fill(ix, iy, ix + TILE - 2, iy + TILE - 2,
                    selected ? 0xFF3E5D7A : hover ? 0xFF3A3A3A : 0xFF262626);
            graphics.outline(ix, iy, TILE - 2, TILE - 2, selected ? 0xFFFFFFFF : 0xFF555555);
            graphics.item(stack, ix + 2, iy + 2);
            if (hover) {
                graphics.setTooltipForNextFrame(this.font, stack, mouseX, mouseY);
            }
        }

        drawButton(graphics, pickerX, top + 278, 28, 18, "<", itemPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((itemPage + 1) + "/" + (maxItemPage + 1)), pickerX + 74, top + 283, 0xFFBDBDBD, false);
        drawButton(graphics, pickerX + 148, top + 278, 28, 18, ">", itemPage < maxItemPage, mouseX, mouseY);

        RecipeEditorSnapshot.RecipeEntry entry = selectedEntry();
        drawButton(graphics, x, top + 312, 110, 22, "Save", canSave(), mouseX, mouseY);
        drawButton(graphics, x + 118, top + 312, 110, 22,
                entry != null && entry.disabled() ? "Enable" : "Disable",
                entry != null, mouseX, mouseY);
        drawButton(graphics, x + 236, top + 312, 118, 22, "Restore original",
                entry != null && entry.hasResourceLayer(), mouseX, mouseY);
        drawButton(graphics, x + 362, top + 312, 118, 22, "Remove custom",
                entry != null && entry.hasServerLayer(), mouseX, mouseY);

        if (entry != null && !creatingNew) {
            String status = "Origin: " + entry.origin().toLowerCase(Locale.ROOT)
                    + (entry.disabled() ? "  •  disabled" : "  •  active");
            graphics.text(this.font, Component.literal(status), x, top + 344,
                    entry.disabled() ? 0xFFFFB070 : 0xFF9AD59A, false);
        } else {
            graphics.text(this.font, Component.literal("New recipes are stored in this world, not written to datapack JSON."),
                    x, top + 344, 0xFF888888, false);
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
        int x = left + LIST_W + 18;

        List<RecipeEditorSnapshot.RecipeEntry> recipes = filteredRecipes();
        int start = recipePage * RECIPE_ROWS;
        int rowY = top + 55;
        for (int i = 0; i < RECIPE_ROWS && start + i < recipes.size(); i++) {
            int y = rowY + i * ROW_H;
            if (hit(mx, my, left + 8, y, LIST_W - 16, ROW_H - 1)) {
                selectRecipe(recipes.get(start + i));
                return true;
            }
        }

        if (hit(mx, my, left + 8, top + 298, 90, 20)) {
            beginNewRecipe();
            return true;
        }
        if (hit(mx, my, left + 105, top + 298, 90, 20) && selectedEntry() != null) {
            duplicateSelected();
            return true;
        }
        if (hit(mx, my, left + 8, top + 324, 28, 18) && recipePage > 0) {
            recipePage--;
            return true;
        }
        int maxRecipePage = Math.max(0, (recipes.size() - 1) / RECIPE_ROWS);
        if (hit(mx, my, left + 167, top + 324, 28, 18) && recipePage < maxRecipePage) {
            recipePage++;
            return true;
        }

        if (hit(mx, my, x, top + 88, 180, 20) && !snapshot.knappingTypes().isEmpty()) {
            cycleType();
            return true;
        }

        int gridX = x;
        int gridY = top + 135;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < 5; col++) {
                int tx = gridX + col * TILE;
                int ty = gridY + row * TILE;
                if (hit(mx, my, tx, ty, TILE - 2, TILE - 2)) {
                    int index = row * 5 + col;
                    pattern[index] = !pattern[index];
                    return true;
                }
            }
        }

        List<Item> items = filteredItems();
        int maxItemPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        int itemStart = itemPage * ITEMS_PER_PAGE;
        int pickerX = x + 139;
        int pickerY = top + 184;
        for (int i = 0; i < ITEMS_PER_PAGE && itemStart + i < items.size(); i++) {
            int col = i % ITEM_COLS;
            int row = i / ITEM_COLS;
            int ix = pickerX + col * TILE;
            int iy = pickerY + row * TILE;
            if (hit(mx, my, ix, iy, TILE - 2, TILE - 2)) {
                selectedResult = BuiltInRegistries.ITEM.getKey(items.get(itemStart + i));
                return true;
            }
        }

        if (hit(mx, my, pickerX, top + 278, 28, 18) && itemPage > 0) {
            itemPage--;
            return true;
        }
        if (hit(mx, my, pickerX + 148, top + 278, 28, 18) && itemPage < maxItemPage) {
            itemPage++;
            return true;
        }

        RecipeEditorSnapshot.RecipeEntry entry = selectedEntry();
        if (hit(mx, my, x, top + 312, 110, 22) && canSave()) {
            sendSave();
            return true;
        }
        if (hit(mx, my, x + 118, top + 312, 110, 22) && entry != null) {
            sendSetDisabled(entry.id(), !entry.disabled());
            return true;
        }
        if (hit(mx, my, x + 236, top + 312, 118, 22) && entry != null) {
            sendSimpleAction("restore_original", entry.id());
            return true;
        }
        if (hit(mx, my, x + 362, top + 312, 118, 22) && entry != null && entry.hasServerLayer()) {
            sendSimpleAction("remove_override", entry.id());
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void selectRecipe(RecipeEditorSnapshot.RecipeEntry entry) {
        selectedRecipeId = entry.id();
        creatingNew = false;
        recipeId.setValue(entry.id());
        selectedType = entry.knappingType();
        selectedResult = parseOrAir(entry.resultItem());
        resultCount.setValue(Integer.toString(entry.resultCount()));
        setPattern(entry.pattern());
        itemPage = 0;
    }

    private boolean selectRecipeById(String id) {
        if (id == null) {
            return false;
        }
        for (RecipeEditorSnapshot.RecipeEntry entry : snapshot.recipes()) {
            if (entry.id().equals(id)) {
                if (recipeId != null) {
                    selectRecipe(entry);
                } else {
                    selectedRecipeId = id;
                }
                return true;
            }
        }
        return false;
    }

    private void beginNewRecipe() {
        creatingNew = true;
        selectedRecipeId = null;
        recipeId.setValue(nextFreeId("simpleknapping:custom_recipe"));
        selectedType = snapshot.knappingTypes().isEmpty() ? "" : snapshot.knappingTypes().get(0);
        selectedResult = Identifier.withDefaultNamespace("flint");
        resultCount.setValue("1");
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = true;
        }
        itemPage = 0;
    }

    private void duplicateSelected() {
        RecipeEditorSnapshot.RecipeEntry entry = selectedEntry();
        if (entry == null) {
            return;
        }
        creatingNew = true;
        selectedRecipeId = null;
        recipeId.setValue(nextFreeId(entry.id() + "_copy"));
        selectedType = entry.knappingType();
        selectedResult = parseOrAir(entry.resultItem());
        resultCount.setValue(Integer.toString(entry.resultCount()));
        setPattern(entry.pattern());
    }

    private String nextFreeId(String base) {
        String candidate = base;
        int suffix = 2;
        while (recipeIdExists(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private boolean recipeIdExists(String id) {
        return snapshot.recipes().stream().anyMatch(entry -> entry.id().equals(id));
    }

    private void cycleType() {
        List<String> types = snapshot.knappingTypes();
        if (types.isEmpty()) {
            selectedType = "";
            return;
        }
        int index = types.indexOf(selectedType);
        selectedType = types.get((index + 1 + types.size()) % types.size());
    }

    private void setPattern(List<String> rows) {
        for (int i = 0; i < pattern.length; i++) {
            pattern[i] = false;
        }
        for (int row = 0; row < Math.min(5, rows.size()); row++) {
            String value = rows.get(row);
            for (int col = 0; col < Math.min(5, value.length()); col++) {
                pattern[row * 5 + col] = value.charAt(col) == 'X';
            }
        }
    }

    private List<String> patternRows() {
        List<String> rows = new ArrayList<>(5);
        for (int row = 0; row < 5; row++) {
            StringBuilder builder = new StringBuilder(5);
            for (int col = 0; col < 5; col++) {
                builder.append(pattern[row * 5 + col] ? 'X' : ' ');
            }
            rows.add(builder.toString());
        }
        return rows;
    }

    private boolean canSave() {
        if (recipeId == null || resultCount == null || selectedType.isBlank()) {
            return false;
        }
        try {
            Identifier.parse(recipeId.getValue().trim());
            int count = Integer.parseInt(resultCount.getValue().trim());
            return count >= 1 && count <= 99 && selectedResult != null && !selectedResult.equals(Identifier.withDefaultNamespace("air"));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void sendSave() {
        JsonObject root = new JsonObject();
        root.addProperty("action", "save");
        root.addProperty("id", recipeId.getValue().trim());
        root.addProperty("knapping_type", selectedType);
        root.addProperty("result_item", selectedResult.toString());
        root.addProperty("result_count", Integer.parseInt(resultCount.getValue().trim()));
        JsonArray rows = new JsonArray();
        patternRows().forEach(rows::add);
        root.add("pattern", rows);
        ClientPacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
    }

    private void sendSetDisabled(String id, boolean disabled) {
        JsonObject root = new JsonObject();
        root.addProperty("action", "set_disabled");
        root.addProperty("id", id);
        root.addProperty("disabled", disabled);
        ClientPacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
    }

    private void sendSimpleAction(String action, String id) {
        JsonObject root = new JsonObject();
        root.addProperty("action", action);
        root.addProperty("id", id);
        ClientPacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
    }

    private RecipeEditorSnapshot.RecipeEntry selectedEntry() {
        if (creatingNew || selectedRecipeId == null) {
            return null;
        }
        return snapshot.recipes().stream()
                .filter(entry -> entry.id().equals(selectedRecipeId))
                .findFirst()
                .orElse(null);
    }

    private List<RecipeEditorSnapshot.RecipeEntry> filteredRecipes() {
        String query = recipeSearch == null ? "" : recipeSearch.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return snapshot.recipes();
        }
        return snapshot.recipes().stream()
                .filter(entry -> entry.id().toLowerCase(Locale.ROOT).contains(query)
                        || entry.knappingType().toLowerCase(Locale.ROOT).contains(query)
                        || entry.resultItem().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<Item> filteredItems() {
        String query = resultSearch == null ? "" : resultSearch.getValue().trim().toLowerCase(Locale.ROOT);
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item != Items.AIR)
                .filter(item -> {
                    if (query.isEmpty()) {
                        return true;
                    }
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    ItemStack stack = new ItemStack(item);
                    return id.toString().toLowerCase(Locale.ROOT).contains(query)
                            || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
                })
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList();
    }

    private ItemStack stackFor(Identifier id) {
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Identifier parseOrAir(String value) {
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ignored) {
            return Identifier.withDefaultNamespace("air");
        }
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

    private void drawButton(GuiGraphicsExtractor graphics,
                            int x, int y, int width, int height,
                            String label, boolean enabled,
                            int mouseX, int mouseY) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, width, height);
        int fill = !enabled ? 0xFF252525 : hovered ? 0xFF4A4A4A : 0xFF353535;
        int border = !enabled ? 0xFF3B3B3B : hovered ? 0xFFFFFFFF : 0xFF777777;
        int text = enabled ? 0xFFFFFFFF : 0xFF777777;
        graphics.fill(x, y, x + width, y + height, fill);
        graphics.outline(x, y, width, height, border);
        graphics.centeredText(this.font, Component.literal(trim(label, Math.max(4, width / 6))), x + width / 2, y + 6, text);
    }

    private static String trim(String value, int maxChars) {
        if (value == null || value.length() <= maxChars) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(1, maxChars - 1)) + "…";
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
