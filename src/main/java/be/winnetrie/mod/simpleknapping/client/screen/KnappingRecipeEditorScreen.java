package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.admin.RecipeEditorSnapshot;
import be.winnetrie.mod.simpleknapping.network.RecipeEditorPayload;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Admin/OP-facing recipe manager. This is intentionally a normal client screen,
 * not a container screen: all authority remains on the server and every write
 * is validated again by RecipeEditorNetwork.
 */
@SuppressWarnings("null")
public final class KnappingRecipeEditorScreen extends Screen {
    // Roughly 20% smaller than the original dev1.3 editor while keeping all controls readable.
    private static final int PANEL_W = 580;
    private static final int PANEL_H = 320;
    private static final int LIST_W = 165;
    private static final int ROW_H = 15;
    private static final int RECIPE_ROWS = 13;
    private static final int TILE = 18;
    private static final int ITEM_COLS = 8;
    private static final int ITEM_ROWS = 4;
    private static final int ITEMS_PER_PAGE = ITEM_COLS * ITEM_ROWS;

    private RecipeEditorSnapshot snapshot;
    private String selectedRecipeId;

    private EditBox recipeSearch;
    private EditBox recipeId;
    private EditBox resultSearch;
    private EditBox resultCount;
    private EditBox materialAmount;

    private final boolean[] pattern = new boolean[25];
    private String selectedType = "";
    private ResourceLocation selectedMaterial = ResourceLocation.withDefaultNamespace("clay");
    private ResourceLocation selectedResult = ResourceLocation.withDefaultNamespace("flint");
    private PickerTarget pickerTarget = PickerTarget.RESULT;

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
        int editorX = left + LIST_W + 14;

        recipeSearch = new EditBox(this.font, left + 8, top + 28, LIST_W - 16, 18, Component.literal("Recipe search"));
        recipeSearch.setMaxLength(100);
        addRenderableWidget(recipeSearch);

        recipeId = new EditBox(this.font, editorX, top + 43, 268, 18, Component.literal("Recipe id"));
        recipeId.setMaxLength(160);
        addRenderableWidget(recipeId);

        materialAmount = new EditBox(this.font, editorX + 278, top + 43, 48, 18, Component.literal("Material amount"));
        materialAmount.setMaxLength(2);
        addRenderableWidget(materialAmount);

        resultSearch = new EditBox(this.font, editorX + 108, top + 138, 210, 18, Component.literal("Item search"));
        resultSearch.setMaxLength(100);
        addRenderableWidget(resultSearch);

        resultCount = new EditBox(this.font, editorX + 40, top + 231, 42, 18, Component.literal("Result count"));
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
        int editorX = left + LIST_W + 14;

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        GuiCompat.outline(graphics, left, top, right - left, bottom - top, 0xFF6A6A6A);

        graphics.drawString(this.font, Component.literal("Knapping Recipe Manager"), left + 8, top + 8, 0xFFFFFFFF, false);
        graphics.drawString(this.font, Component.literal("Recipes"), left + 8, top + 18, 0xFFBDBDBD, false);
        graphics.fill(left + LIST_W, top + 7, left + LIST_W + 1, bottom - 7, 0xFF454545);

        drawRecipeList(graphics, mouseX, mouseY, left, top);
        drawEditor(graphics, mouseX, mouseY, editorX, top);
        drawButton(graphics, editorX + 311, top + 8, 70, 18, "Types", true, mouseX, mouseY);

        if (!snapshot.notice().isBlank()) {
            int noticeColor = snapshot.noticeError() ? 0xFFFF7070 : 0xFF80FF80;
            graphics.drawWordWrap(
                    this.font,
                    Component.literal(snapshot.notice()),
                    editorX,
                    top + 301,
                    Math.max(120, right - editorX - 8),
                    noticeColor
            );
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void drawRecipeList(GuiGraphics graphics, int mouseX, int mouseY, int left, int top) {
        List<RecipeEditorSnapshot.RecipeEntry> filtered = filteredRecipes();
        int maxPage = Math.max(0, (filtered.size() - 1) / RECIPE_ROWS);
        recipePage = Math.min(recipePage, maxPage);

        int start = recipePage * RECIPE_ROWS;
        int rowY = top + 50;
        for (int i = 0; i < RECIPE_ROWS && start + i < filtered.size(); i++) {
            RecipeEditorSnapshot.RecipeEntry entry = filtered.get(start + i);
            int y = rowY + i * ROW_H;
            boolean selected = entry.id().equals(selectedRecipeId) && !creatingNew;
            boolean hovered = hit(mouseX, mouseY, left + 7, y, LIST_W - 14, ROW_H - 1);

            if (selected) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF303030);
            }

            int color = entry.disabled() ? 0xFF888888 : 0xFFE5E5E5;
            graphics.drawString(this.font, trim(entry.id(), 21), left + 10, y + 3, color, false);

            String badge = switch (entry.origin()) {
                case "CUSTOM" -> "C";
                case "OVERRIDE" -> "O";
                default -> "R";
            };
            graphics.drawString(this.font, badge, left + LIST_W - 17, y + 3,
                    "RESOURCE".equals(entry.origin()) ? 0xFFB0B0B0 : 0xFFFFD66B, false);
        }

        drawButton(graphics, left + 7, top + 250, 70, 18, "New", true, mouseX, mouseY);
        drawButton(graphics, left + 84, top + 250, 74, 18, "Duplicate", selectedEntry() != null, mouseX, mouseY);
        drawButton(graphics, left + 7, top + 274, 28, 18, "<", recipePage > 0, mouseX, mouseY);
        graphics.drawString(this.font, Component.literal((recipePage + 1) + "/" + (maxPage + 1)), left + 64, top + 279, 0xFFBDBDBD, false);
        drawButton(graphics, left + 130, top + 274, 28, 18, ">", recipePage < maxPage, mouseX, mouseY);

        graphics.drawString(this.font, Component.literal("R resource  C custom"), left + 8, top + 298, 0xFF777777, false);
        graphics.drawString(this.font, Component.literal("O override"), left + 8, top + 308, 0xFF777777, false);
    }

    private void drawEditor(GuiGraphics graphics, int mouseX, int mouseY, int x, int top) {
        graphics.drawString(this.font, Component.literal(creatingNew ? "New recipe" : "Edit recipe"), x, top + 17, 0xFFFFFFFF, false);

        graphics.drawString(this.font, Component.literal("Recipe ID"), x, top + 32, 0xFFBDBDBD, false);
        graphics.drawString(this.font, Component.literal("Amount"), x + 278, top + 32, 0xFFBDBDBD, false);

        graphics.drawString(this.font, Component.literal("Knapping type"), x, top + 68, 0xFFBDBDBD, false);
        graphics.drawString(this.font, Component.literal("Recipe material"), x + 153, top + 68, 0xFFBDBDBD, false);
        drawButton(graphics, x, top + 79, 145, 18, selectedType.isBlank() ? "No types loaded" : selectedType,
                !snapshot.knappingTypes().isEmpty(), mouseX, mouseY);
        drawButton(graphics, x + 153, top + 79, 215, 18, selectedMaterial.toString(), true, mouseX, mouseY);

        graphics.drawString(this.font, Component.literal("5 x 5 pattern"), x, top + 106, 0xFFBDBDBD, false);
        int gridX = x;
        int gridY = top + 120;
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
                GuiCompat.outline(graphics, tileX, tileY, TILE - 2, TILE - 2, 0xFF666666);
            }
        }

        // Result preview and count live directly below the pattern, keeping the main
        // recipe definition together instead of spreading it across the editor.
        graphics.drawString(this.font, Component.literal("Result"), x, top + 219, 0xFFBDBDBD, false);
        graphics.drawString(this.font, Component.literal("Count"), x + 40, top + 219, 0xFFBDBDBD, false);
        ItemStack resultStack = stackFor(selectedResult);
        graphics.fill(x, top + 230, x + 32, top + 262, 0xFF292929);
        GuiCompat.outline(graphics, x, top + 230, 32, 32, 0xFF777777);
        if (!resultStack.isEmpty()) {
            graphics.renderItem(resultStack, x + 8, top + 238);
            graphics.renderItemDecorations(this.font, resultStack, x + 8, top + 238);
            if (hit(mouseX, mouseY, x, top + 230, 32, 32)) {
                graphics.renderTooltip(this.font, resultStack, mouseX, mouseY);
            }
        }

        int pickerX = x + 108;
        graphics.drawString(this.font, Component.literal("Item picker"), pickerX, top + 106, 0xFFBDBDBD, false);
        drawButton(graphics, pickerX, top + 116, 68, 18, "Result", pickerTarget != PickerTarget.RESULT, mouseX, mouseY);
        drawButton(graphics, pickerX + 72, top + 116, 72, 18, "Material", pickerTarget != PickerTarget.MATERIAL, mouseX, mouseY);

        List<Item> items = filteredItems();
        int maxItemPage = Math.max(0, (items.size() - 1) / ITEMS_PER_PAGE);
        itemPage = Math.min(itemPage, maxItemPage);
        int itemStart = itemPage * ITEMS_PER_PAGE;
        int pickerY = top + 162;

        for (int i = 0; i < ITEMS_PER_PAGE && itemStart + i < items.size(); i++) {
            Item item = items.get(itemStart + i);
            ItemStack stack = new ItemStack(item);
            int col = i % ITEM_COLS;
            int row = i / ITEM_COLS;
            int ix = pickerX + col * TILE;
            int iy = pickerY + row * TILE;
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
            boolean selected = itemId.equals(selectedPickerId());
            boolean hover = hit(mouseX, mouseY, ix, iy, TILE - 2, TILE - 2);

            graphics.fill(ix, iy, ix + TILE - 2, iy + TILE - 2,
                    selected ? 0xFF3E5D7A : hover ? 0xFF3A3A3A : 0xFF262626);
            GuiCompat.outline(graphics, ix, iy, TILE - 2, TILE - 2, selected ? 0xFFFFFFFF : 0xFF555555);
            graphics.renderItem(stack, ix, iy);
            if (hover) {
                graphics.renderTooltip(this.font, stack, mouseX, mouseY);
            }
        }

        drawButton(graphics, pickerX, top + 239, 28, 18, "<", itemPage > 0, mouseX, mouseY);
        graphics.drawString(this.font, Component.literal((itemPage + 1) + "/" + (maxItemPage + 1)), pickerX + 62, top + 244, 0xFFBDBDBD, false);
        drawButton(graphics, pickerX + 116, top + 239, 28, 18, ">", itemPage < maxItemPage, mouseX, mouseY);

        RecipeEditorSnapshot.RecipeEntry entry = selectedEntry();
        int actionX = x + 200;
        drawButton(graphics, actionX, top + 259, 86, 18, "Save", canSave(), mouseX, mouseY);
        drawButton(graphics, actionX + 92, top + 259, 86, 18,
                entry != null && entry.disabled() ? "Enable" : "Disable",
                entry != null, mouseX, mouseY);
        drawButton(graphics, actionX, top + 280, 86, 18, "Restore original",
                entry != null && entry.hasResourceLayer(), mouseX, mouseY);
        drawButton(graphics, actionX + 92, top + 280, 86, 18, "Remove custom",
                entry != null && entry.hasServerLayer(), mouseX, mouseY);

        if (snapshot.notice().isBlank()) {
            if (entry != null && !creatingNew) {
                String status = "Origin: " + entry.origin().toLowerCase(Locale.ROOT)
                        + (entry.disabled() ? "  •  disabled" : "  •  active");
                graphics.drawString(this.font, Component.literal(status), x, top + 303,
                        entry.disabled() ? 0xFFFFB070 : 0xFF9AD59A, false);
            } else {
                graphics.drawString(this.font, Component.literal("New recipes are stored in this world."),
                        x, top + 303, 0xFF888888, false);
            }
        }
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
        int x = left + LIST_W + 14;

        if (hit(mx, my, x + 311, top + 8, 70, 18)) {
            if (this.minecraft != null) {
                this.minecraft.setScreen(new KnappingTypeEditorScreen(snapshot));
            }
            return true;
        }

        List<RecipeEditorSnapshot.RecipeEntry> recipes = filteredRecipes();
        int start = recipePage * RECIPE_ROWS;
        int rowY = top + 50;
        for (int i = 0; i < RECIPE_ROWS && start + i < recipes.size(); i++) {
            int y = rowY + i * ROW_H;
            if (hit(mx, my, left + 7, y, LIST_W - 14, ROW_H - 1)) {
                selectRecipe(recipes.get(start + i));
                return true;
            }
        }

        if (hit(mx, my, left + 7, top + 250, 70, 18)) {
            beginNewRecipe();
            return true;
        }
        if (hit(mx, my, left + 84, top + 250, 74, 18) && selectedEntry() != null) {
            duplicateSelected();
            return true;
        }
        if (hit(mx, my, left + 7, top + 274, 28, 18) && recipePage > 0) {
            recipePage--;
            return true;
        }
        int maxRecipePage = Math.max(0, (recipes.size() - 1) / RECIPE_ROWS);
        if (hit(mx, my, left + 130, top + 274, 28, 18) && recipePage < maxRecipePage) {
            recipePage++;
            return true;
        }

        if (hit(mx, my, x, top + 79, 145, 18) && !snapshot.knappingTypes().isEmpty()) {
            cycleType();
            return true;
        }
        if (hit(mx, my, x + 153, top + 79, 215, 18)) {
            pickerTarget = PickerTarget.MATERIAL;
            itemPage = 0;
            return true;
        }
        if (hit(mx, my, x + 108, top + 116, 68, 18)) {
            pickerTarget = PickerTarget.RESULT;
            itemPage = 0;
            return true;
        }
        if (hit(mx, my, x + 180, top + 116, 72, 18)) {
            pickerTarget = PickerTarget.MATERIAL;
            itemPage = 0;
            return true;
        }

        int gridX = x;
        int gridY = top + 120;
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
        int pickerX = x + 108;
        int pickerY = top + 162;
        for (int i = 0; i < ITEMS_PER_PAGE && itemStart + i < items.size(); i++) {
            int col = i % ITEM_COLS;
            int row = i / ITEM_COLS;
            int ix = pickerX + col * TILE;
            int iy = pickerY + row * TILE;
            if (hit(mx, my, ix, iy, TILE - 2, TILE - 2)) {
                setPickerSelection(BuiltInRegistries.ITEM.getKey(items.get(itemStart + i)));
                return true;
            }
        }

        if (hit(mx, my, pickerX, top + 239, 28, 18) && itemPage > 0) {
            itemPage--;
            return true;
        }
        if (hit(mx, my, pickerX + 116, top + 239, 28, 18) && itemPage < maxItemPage) {
            itemPage++;
            return true;
        }

        RecipeEditorSnapshot.RecipeEntry entry = selectedEntry();
        int actionX = x + 200;
        if (hit(mx, my, actionX, top + 259, 86, 18) && canSave()) {
            sendSave();
            return true;
        }
        if (hit(mx, my, actionX + 92, top + 259, 86, 18) && entry != null) {
            sendSetDisabled(entry.id(), !entry.disabled());
            return true;
        }
        if (hit(mx, my, actionX, top + 280, 86, 18) && entry != null && entry.hasResourceLayer()) {
            sendSimpleAction("restore_original", entry.id());
            return true;
        }
        if (hit(mx, my, actionX + 92, top + 280, 86, 18) && entry != null && entry.hasServerLayer()) {
            sendSimpleAction("remove_override", entry.id());
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void selectRecipe(RecipeEditorSnapshot.RecipeEntry entry) {
        selectedRecipeId = entry.id();
        creatingNew = false;
        recipeId.setValue(entry.id());
        selectedType = entry.knappingType();
        selectedMaterial = parseOrAir(entry.material());
        selectedResult = parseOrAir(entry.resultItem());
        materialAmount.setValue(Integer.toString(entry.materialCost()));
        resultCount.setValue(Integer.toString(entry.resultCount()));
        setPattern(entry.pattern());
        pickerTarget = PickerTarget.RESULT;
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
        applyTypeDefaults();
        selectedResult = ResourceLocation.withDefaultNamespace("flint");
        resultCount.setValue("1");
        pickerTarget = PickerTarget.RESULT;
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
        selectedMaterial = parseOrAir(entry.material());
        selectedResult = parseOrAir(entry.resultItem());
        materialAmount.setValue(Integer.toString(entry.materialCost()));
        resultCount.setValue(Integer.toString(entry.resultCount()));
        setPattern(entry.pattern());
        pickerTarget = PickerTarget.RESULT;
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
        if (creatingNew) {
            applyTypeDefaults();
        }
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
        if (recipeId == null || resultCount == null || materialAmount == null || selectedType.isBlank()) {
            return false;
        }
        try {
            ResourceLocation.parse(recipeId.getValue().trim());
            int count = Integer.parseInt(resultCount.getValue().trim());
            int amount = Integer.parseInt(materialAmount.getValue().trim());
            ResourceLocation air = ResourceLocation.withDefaultNamespace("air");
            return count >= 1 && count <= 99
                    && amount >= 1 && amount <= 99
                    && selectedMaterial != null && !selectedMaterial.equals(air)
                    && selectedResult != null && !selectedResult.equals(air);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void sendSave() {
        JsonObject root = new JsonObject();
        root.addProperty("action", "save");
        root.addProperty("id", recipeId.getValue().trim());
        root.addProperty("knapping_type", selectedType);
        root.addProperty("material", selectedMaterial.toString());
        root.addProperty("material_cost", Integer.parseInt(materialAmount.getValue().trim()));
        root.addProperty("result_item", selectedResult.toString());
        root.addProperty("result_count", Integer.parseInt(resultCount.getValue().trim()));
        JsonArray rows = new JsonArray();
        patternRows().forEach(rows::add);
        root.add("pattern", rows);
        PacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
    }

    private void sendSetDisabled(String id, boolean disabled) {
        JsonObject root = new JsonObject();
        root.addProperty("action", "set_disabled");
        root.addProperty("id", id);
        root.addProperty("disabled", disabled);
        PacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
    }

    private void sendSimpleAction(String action, String id) {
        JsonObject root = new JsonObject();
        root.addProperty("action", action);
        root.addProperty("id", id);
        PacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
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
                        || entry.material().toLowerCase(Locale.ROOT).contains(query)
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
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    ItemStack stack = new ItemStack(item);
                    return id.toString().toLowerCase(Locale.ROOT).contains(query)
                            || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query);
                })
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()))
                .toList();
    }

    private ResourceLocation selectedPickerId() {
        return pickerTarget == PickerTarget.MATERIAL ? selectedMaterial : selectedResult;
    }

    private void setPickerSelection(ResourceLocation id) {
        if (pickerTarget == PickerTarget.MATERIAL) {
            selectedMaterial = id;
        } else {
            selectedResult = id;
        }
    }

    private void applyTypeDefaults() {
        RecipeEditorSnapshot.TypeEntry type = snapshot.typeEntries().stream()
                .filter(entry -> entry.id().equals(selectedType))
                .findFirst()
                .orElse(null);
        if (type == null) {
            selectedMaterial = ResourceLocation.withDefaultNamespace("clay");
            if (materialAmount != null) {
                materialAmount.setValue("1");
            }
            return;
        }
        selectedMaterial = parseOrAir(type.material());
        if (materialAmount != null) {
            materialAmount.setValue(Integer.toString(type.materialCost()));
        }
    }

    private ItemStack stackFor(ResourceLocation id) {
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private ResourceLocation parseOrAir(String value) {
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException ignored) {
            return ResourceLocation.withDefaultNamespace("air");
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

    private void drawButton(GuiGraphics graphics,
                            int x, int y, int width, int height,
                            String label, boolean enabled,
                            int mouseX, int mouseY) {
        boolean hovered = enabled && hit(mouseX, mouseY, x, y, width, height);
        int fill = !enabled ? 0xFF252525 : hovered ? 0xFF4A4A4A : 0xFF353535;
        int border = !enabled ? 0xFF3B3B3B : hovered ? 0xFFFFFFFF : 0xFF777777;
        int text = enabled ? 0xFFFFFFFF : 0xFF777777;
        graphics.fill(x, y, x + width, y + height, fill);
        GuiCompat.outline(graphics, x, y, width, height, border);
        graphics.drawCenteredString(this.font, Component.literal(trim(label, Math.max(4, width / 6))), x + width / 2, y + 6, text);
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

    private enum PickerTarget {
        RESULT,
        MATERIAL
    }
}
