package be.winnetrie.mod.simpleknapping.client.screen;

import be.winnetrie.mod.simpleknapping.admin.RecipeEditorSnapshot;
import be.winnetrie.mod.simpleknapping.network.RecipeEditorPayload;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Admin/OP-facing manager for world-persistent custom knapping types. */
public final class KnappingTypeEditorScreen extends Screen {
    // Keep all admin manager panels the same size as the compact recipe manager.
    private static final int PANEL_W = 580;
    private static final int PANEL_H = 320;
    private static final int LIST_W = 165;
    private static final int ROW_H = 15;
    private static final int TYPE_ROWS = 13;
    private static final int TILE = 18;
    private static final int PICKER_COLS = 8;
    private static final int PICKER_ROWS = 3;
    private static final int PICKER_PER_PAGE = PICKER_COLS * PICKER_ROWS;

    private static final Identifier DEFAULT_TOOL = Identifier.fromNamespaceAndPath("simpleknapping", "flint_knapping_tool");
    private static final Identifier DEFAULT_MATERIAL = Identifier.withDefaultNamespace("flint");
    private static final Identifier DEFAULT_TEXTURE_BLOCK = Identifier.withDefaultNamespace("clay");

    private RecipeEditorSnapshot snapshot;
    private String selectedTypeId;

    private EditBox typeSearch;
    private EditBox typeId;
    private EditBox materialAmount;
    private EditBox pickerSearch;

    private Identifier selectedTool = DEFAULT_TOOL;
    private Identifier selectedMaterial = DEFAULT_MATERIAL;
    private Identifier selectedTextureBlock = DEFAULT_TEXTURE_BLOCK;
    private PickerTarget pickerTarget = PickerTarget.TOOL;

    private int typePage;
    private int pickerPage;
    private boolean creatingNew;

    public KnappingTypeEditorScreen(RecipeEditorSnapshot snapshot) {
        super(Component.literal("Knapping Type Manager"));
        this.snapshot = snapshot;
    }

    @Override
    protected void init() {
        int left = panelLeft();
        int top = panelTop();
        int editorX = left + LIST_W + 14;

        typeSearch = new EditBox(this.font, left + 8, top + 28, LIST_W - 16, 18, Component.literal("Type search"));
        typeSearch.setMaxLength(100);
        addRenderableWidget(typeSearch);

        typeId = new EditBox(this.font, editorX, top + 43, 268, 18, Component.literal("Type id"));
        typeId.setMaxLength(160);
        addRenderableWidget(typeId);

        materialAmount = new EditBox(this.font, editorX + 278, top + 43, 48, 18, Component.literal("Amount"));
        materialAmount.setMaxLength(2);
        addRenderableWidget(materialAmount);

        pickerSearch = new EditBox(this.font, editorX + 202, top + 103, 180, 18, Component.literal("Picker search"));
        pickerSearch.setMaxLength(100);
        addRenderableWidget(pickerSearch);

        if (selectedTypeId != null) {
            selectTypeById(selectedTypeId);
        } else if (!snapshot.typeEntries().isEmpty()) {
            selectType(snapshot.typeEntries().get(0));
        } else {
            beginNewType();
        }
    }

    public void applyServerSnapshot(RecipeEditorSnapshot newSnapshot) {
        String keepId = typeId != null ? typeId.getValue().trim() : selectedTypeId;
        this.snapshot = newSnapshot;
        this.typePage = 0;

        if (keepId != null && selectTypeById(keepId)) {
            return;
        }
        if (!newSnapshot.typeEntries().isEmpty()) {
            selectType(newSnapshot.typeEntries().get(0));
        } else {
            beginNewType();
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int left = panelLeft();
        int top = panelTop();
        int right = left + Math.min(PANEL_W, this.width - 12);
        int bottom = top + Math.min(PANEL_H, this.height - 12);
        int editorX = left + LIST_W + 14;

        graphics.fill(0, 0, this.width, this.height, 0xB0000000);
        graphics.fill(left, top, right, bottom, 0xFF171717);
        graphics.outline(left, top, right - left, bottom - top, 0xFF6A6A6A);

        graphics.text(this.font, Component.literal("Knapping Type Manager"), left + 8, top + 8, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("Types"), left + 8, top + 18, 0xFFBDBDBD, false);
        graphics.fill(left + LIST_W, top + 7, left + LIST_W + 1, bottom - 7, 0xFF454545);

        drawTypeList(graphics, mouseX, mouseY, left, top);
        drawEditor(graphics, mouseX, mouseY, editorX, top);
        drawButton(graphics, editorX + 311, top + 8, 70, 18, "Recipes", true, mouseX, mouseY);

        if (!snapshot.notice().isBlank()) {
            int noticeColor = snapshot.noticeError() ? 0xFFFF7070 : 0xFF80FF80;
            graphics.textWithWordWrap(
                    this.font,
                    Component.literal(snapshot.notice()),
                    editorX,
                    top + 301,
                    Math.max(120, right - editorX - 8),
                    noticeColor
            );
        }
    }

    private void drawTypeList(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int left, int top) {
        List<RecipeEditorSnapshot.TypeEntry> filtered = filteredTypes();
        int maxPage = Math.max(0, (filtered.size() - 1) / TYPE_ROWS);
        typePage = Math.min(typePage, maxPage);

        int start = typePage * TYPE_ROWS;
        int rowY = top + 50;
        for (int i = 0; i < TYPE_ROWS && start + i < filtered.size(); i++) {
            RecipeEditorSnapshot.TypeEntry entry = filtered.get(start + i);
            int y = rowY + i * ROW_H;
            boolean selected = entry.id().equals(selectedTypeId) && !creatingNew;
            boolean hovered = hit(mouseX, mouseY, left + 7, y, LIST_W - 14, ROW_H - 1);

            if (selected) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF3E5D7A);
            } else if (hovered) {
                graphics.fill(left + 7, y, left + LIST_W - 7, y + ROW_H - 1, 0xFF303030);
            }

            int color = entry.disabled() ? 0xFF888888 : 0xFFE5E5E5;
            graphics.text(this.font, trim(entry.id(), 21), left + 10, y + 3, color, false);

            String badge = switch (entry.origin()) {
                case "CUSTOM" -> "C";
                case "OVERRIDE" -> "O";
                default -> "R";
            };
            graphics.text(this.font, badge, left + LIST_W - 17, y + 3,
                    "RESOURCE".equals(entry.origin()) ? 0xFFB0B0B0 : 0xFFFFD66B, false);
        }

        drawButton(graphics, left + 7, top + 250, 70, 18, "New", true, mouseX, mouseY);
        drawButton(graphics, left + 84, top + 250, 74, 18, "Duplicate", selectedEntry() != null, mouseX, mouseY);
        drawButton(graphics, left + 7, top + 274, 28, 18, "<", typePage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((typePage + 1) + "/" + (maxPage + 1)), left + 64, top + 279, 0xFFBDBDBD, false);
        drawButton(graphics, left + 130, top + 274, 28, 18, ">", typePage < maxPage, mouseX, mouseY);
        graphics.text(this.font, Component.literal("R resource  C custom"), left + 8, top + 298, 0xFF777777, false);
        graphics.text(this.font, Component.literal("O override"), left + 8, top + 308, 0xFF777777, false);
    }

    private void drawEditor(GuiGraphicsExtractor graphics, int mouseX, int mouseY, int x, int top) {
        graphics.text(this.font, Component.literal(creatingNew ? "New knapping type" : "Edit knapping type"), x, top + 17, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.literal("Type ID"), x, top + 32, 0xFFBDBDBD, false);
        graphics.text(this.font, Component.literal("Default amount"), x + 278, top + 32, 0xFFBDBDBD, false);

        drawSelectionRow(graphics, mouseX, mouseY, x, top + 68, "Knapping tool", selectedTool, PickerTarget.TOOL);
        drawSelectionRow(graphics, mouseX, mouseY, x, top + 105, "Default recipe material", selectedMaterial, PickerTarget.MATERIAL);
        drawSelectionRow(graphics, mouseX, mouseY, x, top + 142, "Fallback texture block", selectedTextureBlock, PickerTarget.TEXTURE);

        int pickerX = x + 202;
        graphics.text(this.font, Component.literal("Pick: " + pickerTarget.label), pickerX, top + 68, 0xFFBDBDBD, false);
        drawButton(graphics, pickerX, top + 79, 54, 18, "Tool", pickerTarget != PickerTarget.TOOL, mouseX, mouseY);
        drawButton(graphics, pickerX + 58, top + 79, 62, 18, "Material", pickerTarget != PickerTarget.MATERIAL, mouseX, mouseY);
        drawButton(graphics, pickerX + 124, top + 79, 56, 18, "Texture", pickerTarget != PickerTarget.TEXTURE, mouseX, mouseY);

        List<PickerEntry> entries = filteredPickerEntries();
        int maxPickerPage = Math.max(0, (entries.size() - 1) / PICKER_PER_PAGE);
        pickerPage = Math.min(pickerPage, maxPickerPage);
        int start = pickerPage * PICKER_PER_PAGE;
        int gridY = top + 126;

        for (int i = 0; i < PICKER_PER_PAGE && start + i < entries.size(); i++) {
            PickerEntry entry = entries.get(start + i);
            int col = i % PICKER_COLS;
            int row = i / PICKER_COLS;
            int ix = pickerX + col * TILE;
            int iy = gridY + row * TILE;
            boolean selected = entry.id().equals(currentSelection());
            boolean hover = hit(mouseX, mouseY, ix, iy, TILE - 2, TILE - 2);

            graphics.fill(ix, iy, ix + TILE - 2, iy + TILE - 2,
                    selected ? 0xFF3E5D7A : hover ? 0xFF3A3A3A : 0xFF262626);
            graphics.outline(ix, iy, TILE - 2, TILE - 2, selected ? 0xFFFFFFFF : 0xFF555555);
            if (!entry.stack().isEmpty()) {
                graphics.item(entry.stack(), ix, iy);
                if (hover) {
                    graphics.setTooltipForNextFrame(this.font, entry.stack(), mouseX, mouseY);
                }
            }
        }

        drawButton(graphics, pickerX, top + 184, 28, 18, "<", pickerPage > 0, mouseX, mouseY);
        graphics.text(this.font, Component.literal((pickerPage + 1) + "/" + (maxPickerPage + 1)), pickerX + 62, top + 189, 0xFFBDBDBD, false);
        drawButton(graphics, pickerX + 116, top + 184, 28, 18, ">", pickerPage < maxPickerPage, mouseX, mouseY);

        graphics.text(this.font, Component.literal("Block recipe materials use their own block texture."), x, top + 207, 0xFF888888, false);
        graphics.text(this.font, Component.literal("Otherwise fallback is used; invalid/missing -> minecraft:clay."), x, top + 218, 0xFF888888, false);

        RecipeEditorSnapshot.TypeEntry selected = selectedEntry();
        int actionX = x + 200;
        drawButton(graphics, actionX, top + 239, 86, 18, "Save", canSave(), mouseX, mouseY);
        drawButton(graphics, actionX + 92, top + 239, 86, 18,
                selected != null && selected.disabled() ? "Enable" : "Disable",
                selected != null, mouseX, mouseY);
        drawButton(graphics, actionX, top + 260, 86, 18, "Restore original",
                selected != null && selected.hasResourceLayer(), mouseX, mouseY);
        drawButton(graphics, actionX + 92, top + 260, 86, 18, "Remove custom",
                selected != null && selected.hasServerLayer(), mouseX, mouseY);

        if (snapshot.notice().isBlank()) {
            if (selected != null && !creatingNew) {
                String status = "Origin: " + selected.origin().toLowerCase(Locale.ROOT)
                        + (selected.disabled() ? "  •  disabled" : "  •  active");
                graphics.text(this.font, Component.literal(status), x, top + 286,
                        selected.disabled() ? 0xFFFFB070 : 0xFF9AD59A, false);
                if ("RESOURCE".equals(selected.origin()) && !isStandardBlockTexture(selected.resolvedTexture(), selected.textureBlock())) {
                    graphics.text(this.font, Component.literal("Legacy resource texture stays until an override is saved."),
                            x, top + 298, 0xFF888888, false);
                }
            } else {
                graphics.text(this.font, Component.literal("New types are stored in this world."),
                        x, top + 286, 0xFF888888, false);
            }
        }
    }

    private void drawSelectionRow(GuiGraphicsExtractor graphics,
                                  int mouseX, int mouseY,
                                  int x, int y,
                                  String label, Identifier selected,
                                  PickerTarget target) {
        graphics.text(this.font, Component.literal(label), x, y, 0xFFBDBDBD, false);
        boolean active = pickerTarget == target;
        drawButton(graphics, x, y + 10, 170, 18, selected == null ? "None" : selected.toString(), true, mouseX, mouseY);
        if (active) {
            graphics.outline(x - 1, y + 9, 172, 20, 0xFF8CC8FF);
        }

        ItemStack preview = target == PickerTarget.TEXTURE ? stackForBlock(selected) : stackForItem(selected);
        graphics.fill(x + 176, y + 7, x + 198, y + 29, 0xFF292929);
        graphics.outline(x + 176, y + 7, 22, 22, 0xFF666666);
        if (!preview.isEmpty()) {
            graphics.item(preview, x + 179, y + 10);
            if (hit(mouseX, mouseY, x + 176, y + 7, 22, 22)) {
                graphics.setTooltipForNextFrame(this.font, preview, mouseX, mouseY);
            }
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
        int x = left + LIST_W + 14;

        if (hit(mx, my, x + 311, top + 8, 70, 18)) {
            this.minecraft.setScreenAndShow(new KnappingRecipeEditorScreen(snapshot));
            return true;
        }

        List<RecipeEditorSnapshot.TypeEntry> types = filteredTypes();
        int start = typePage * TYPE_ROWS;
        int rowY = top + 50;
        for (int i = 0; i < TYPE_ROWS && start + i < types.size(); i++) {
            int y = rowY + i * ROW_H;
            if (hit(mx, my, left + 7, y, LIST_W - 14, ROW_H - 1)) {
                selectType(types.get(start + i));
                return true;
            }
        }

        if (hit(mx, my, left + 7, top + 250, 70, 18)) {
            beginNewType();
            return true;
        }
        if (hit(mx, my, left + 84, top + 250, 74, 18) && selectedEntry() != null) {
            duplicateSelected();
            return true;
        }
        if (hit(mx, my, left + 7, top + 274, 28, 18) && typePage > 0) {
            typePage--;
            return true;
        }
        int maxTypePage = Math.max(0, (types.size() - 1) / TYPE_ROWS);
        if (hit(mx, my, left + 130, top + 274, 28, 18) && typePage < maxTypePage) {
            typePage++;
            return true;
        }

        if (hit(mx, my, x, top + 78, 170, 18)) {
            setPickerTarget(PickerTarget.TOOL);
            return true;
        }
        if (hit(mx, my, x, top + 115, 170, 18)) {
            setPickerTarget(PickerTarget.MATERIAL);
            return true;
        }
        if (hit(mx, my, x, top + 152, 170, 18)) {
            setPickerTarget(PickerTarget.TEXTURE);
            return true;
        }

        int pickerX = x + 202;
        if (hit(mx, my, pickerX, top + 79, 54, 18)) {
            setPickerTarget(PickerTarget.TOOL);
            return true;
        }
        if (hit(mx, my, pickerX + 58, top + 79, 62, 18)) {
            setPickerTarget(PickerTarget.MATERIAL);
            return true;
        }
        if (hit(mx, my, pickerX + 124, top + 79, 56, 18)) {
            setPickerTarget(PickerTarget.TEXTURE);
            return true;
        }

        List<PickerEntry> entries = filteredPickerEntries();
        int maxPickerPage = Math.max(0, (entries.size() - 1) / PICKER_PER_PAGE);
        int pickerStart = pickerPage * PICKER_PER_PAGE;
        int gridY = top + 126;
        for (int i = 0; i < PICKER_PER_PAGE && pickerStart + i < entries.size(); i++) {
            int col = i % PICKER_COLS;
            int row = i / PICKER_COLS;
            int ix = pickerX + col * TILE;
            int iy = gridY + row * TILE;
            if (hit(mx, my, ix, iy, TILE - 2, TILE - 2)) {
                setCurrentSelection(entries.get(pickerStart + i).id());
                return true;
            }
        }

        if (hit(mx, my, pickerX, top + 184, 28, 18) && pickerPage > 0) {
            pickerPage--;
            return true;
        }
        if (hit(mx, my, pickerX + 116, top + 184, 28, 18) && pickerPage < maxPickerPage) {
            pickerPage++;
            return true;
        }

        RecipeEditorSnapshot.TypeEntry selected = selectedEntry();
        int actionX = x + 200;
        if (hit(mx, my, actionX, top + 239, 86, 18) && canSave()) {
            sendSave();
            return true;
        }
        if (hit(mx, my, actionX + 92, top + 239, 86, 18) && selected != null) {
            sendSetDisabled(selected.id(), !selected.disabled());
            return true;
        }
        if (hit(mx, my, actionX, top + 260, 86, 18) && selected != null && selected.hasResourceLayer()) {
            sendSimpleAction("restore_type_original", selected.id());
            return true;
        }
        if (hit(mx, my, actionX + 92, top + 260, 86, 18) && selected != null && selected.hasServerLayer()) {
            sendSimpleAction("remove_type_override", selected.id());
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    private void selectType(RecipeEditorSnapshot.TypeEntry entry) {
        selectedTypeId = entry.id();
        creatingNew = false;
        typeId.setValue(entry.id());
        selectedTool = parseOrDefault(entry.tool(), DEFAULT_TOOL);
        selectedMaterial = parseOrDefault(entry.material(), DEFAULT_MATERIAL);
        selectedTextureBlock = parseOrDefault(entry.textureBlock(), DEFAULT_TEXTURE_BLOCK);
        materialAmount.setValue(Integer.toString(entry.materialCost()));
        pickerPage = 0;
    }

    private boolean selectTypeById(String id) {
        if (id == null) {
            return false;
        }
        for (RecipeEditorSnapshot.TypeEntry entry : snapshot.typeEntries()) {
            if (entry.id().equals(id)) {
                if (typeId != null) {
                    selectType(entry);
                } else {
                    selectedTypeId = id;
                }
                return true;
            }
        }
        return false;
    }

    private void beginNewType() {
        creatingNew = true;
        selectedTypeId = null;
        typeId.setValue(nextFreeId("simpleknapping:custom_type"));
        selectedTool = registryItemOrFallback(DEFAULT_TOOL, Identifier.withDefaultNamespace("flint"));
        selectedMaterial = DEFAULT_MATERIAL;
        selectedTextureBlock = DEFAULT_TEXTURE_BLOCK;
        materialAmount.setValue("1");
        pickerTarget = PickerTarget.TOOL;
        pickerPage = 0;
    }

    private void duplicateSelected() {
        RecipeEditorSnapshot.TypeEntry entry = selectedEntry();
        if (entry == null) {
            return;
        }
        creatingNew = true;
        selectedTypeId = null;
        typeId.setValue(nextFreeId(entry.id() + "_copy"));
        selectedTool = parseOrDefault(entry.tool(), DEFAULT_TOOL);
        selectedMaterial = parseOrDefault(entry.material(), DEFAULT_MATERIAL);
        selectedTextureBlock = parseOrDefault(entry.textureBlock(), DEFAULT_TEXTURE_BLOCK);
        materialAmount.setValue(Integer.toString(entry.materialCost()));
        pickerPage = 0;
    }

    private String nextFreeId(String base) {
        String candidate = base;
        int suffix = 2;
        while (typeIdExists(candidate)) {
            candidate = base + "_" + suffix++;
        }
        return candidate;
    }

    private boolean typeIdExists(String id) {
        return snapshot.typeEntries().stream().anyMatch(entry -> entry.id().equals(id));
    }

    private void setPickerTarget(PickerTarget target) {
        pickerTarget = target;
        pickerPage = 0;
    }

    private Identifier currentSelection() {
        return switch (pickerTarget) {
            case TOOL -> selectedTool;
            case MATERIAL -> selectedMaterial;
            case TEXTURE -> selectedTextureBlock;
        };
    }

    private void setCurrentSelection(Identifier id) {
        switch (pickerTarget) {
            case TOOL -> selectedTool = id;
            case MATERIAL -> selectedMaterial = id;
            case TEXTURE -> selectedTextureBlock = id;
        }
    }

    private boolean canSave() {
        if (typeId == null || materialAmount == null) {
            return false;
        }
        try {
            Identifier.parse(typeId.getValue().trim());
            int amount = Integer.parseInt(materialAmount.getValue().trim());
            return amount >= 1 && amount <= 99
                    && validItem(selectedTool)
                    && validItem(selectedMaterial)
                    && validBlock(selectedTextureBlock);
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private void sendSave() {
        JsonObject root = new JsonObject();
        root.addProperty("action", "save_type");
        root.addProperty("id", typeId.getValue().trim());
        root.addProperty("tool", selectedTool.toString());
        root.addProperty("material", selectedMaterial.toString());
        root.addProperty("material_cost", Integer.parseInt(materialAmount.getValue().trim()));
        root.addProperty("texture_block", selectedTextureBlock.toString());
        ClientPacketDistributor.sendToServer(RecipeEditorPayload.action(root.toString()));
    }

    private void sendSetDisabled(String id, boolean disabled) {
        JsonObject root = new JsonObject();
        root.addProperty("action", "set_type_disabled");
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

    private RecipeEditorSnapshot.TypeEntry selectedEntry() {
        if (creatingNew || selectedTypeId == null) {
            return null;
        }
        return snapshot.typeEntries().stream()
                .filter(entry -> entry.id().equals(selectedTypeId))
                .findFirst()
                .orElse(null);
    }

    private List<RecipeEditorSnapshot.TypeEntry> filteredTypes() {
        String query = typeSearch == null ? "" : typeSearch.getValue().trim().toLowerCase(Locale.ROOT);
        if (query.isEmpty()) {
            return snapshot.typeEntries();
        }
        return snapshot.typeEntries().stream()
                .filter(entry -> entry.id().toLowerCase(Locale.ROOT).contains(query)
                        || entry.tool().toLowerCase(Locale.ROOT).contains(query)
                        || entry.material().toLowerCase(Locale.ROOT).contains(query)
                        || entry.textureBlock().toLowerCase(Locale.ROOT).contains(query))
                .toList();
    }

    private List<PickerEntry> filteredPickerEntries() {
        String query = pickerSearch == null ? "" : pickerSearch.getValue().trim().toLowerCase(Locale.ROOT);
        List<PickerEntry> entries = new ArrayList<>();

        if (pickerTarget == PickerTarget.TEXTURE) {
            for (Block block : BuiltInRegistries.BLOCK.stream().toList()) {
                if (block == Blocks.AIR) {
                    continue;
                }
                Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                Item item = block.asItem();
                ItemStack stack = item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
                String name = stack.isEmpty() ? id.toString() : stack.getHoverName().getString();
                if (query.isEmpty()
                        || id.toString().toLowerCase(Locale.ROOT).contains(query)
                        || name.toLowerCase(Locale.ROOT).contains(query)) {
                    entries.add(new PickerEntry(id, stack));
                }
            }
        } else {
            for (Item item : BuiltInRegistries.ITEM.stream().toList()) {
                if (item == Items.AIR) {
                    continue;
                }
                Identifier id = BuiltInRegistries.ITEM.getKey(item);
                ItemStack stack = new ItemStack(item);
                if (query.isEmpty()
                        || id.toString().toLowerCase(Locale.ROOT).contains(query)
                        || stack.getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)) {
                    entries.add(new PickerEntry(id, stack));
                }
            }
        }

        entries.sort(Comparator.comparing(entry -> entry.id().toString()));
        return entries;
    }

    private boolean validItem(Identifier id) {
        return id != null && BuiltInRegistries.ITEM.containsKey(id) && BuiltInRegistries.ITEM.getValue(id) != Items.AIR;
    }

    private boolean validBlock(Identifier id) {
        return id != null && BuiltInRegistries.BLOCK.containsKey(id) && BuiltInRegistries.BLOCK.getValue(id) != Blocks.AIR;
    }

    private ItemStack stackForItem(Identifier id) {
        if (!validItem(id)) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }

    private ItemStack stackForBlock(Identifier id) {
        if (!validBlock(id)) {
            return ItemStack.EMPTY;
        }
        Item item = BuiltInRegistries.BLOCK.getValue(id).asItem();
        return item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    private Identifier parseOrDefault(String value, Identifier fallback) {
        try {
            return Identifier.parse(value);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }

    private Identifier registryItemOrFallback(Identifier preferred, Identifier fallback) {
        return validItem(preferred) ? preferred : fallback;
    }

    private boolean isStandardBlockTexture(String resolvedTexture, String textureBlock) {
        try {
            Identifier block = Identifier.parse(textureBlock);
            Identifier expected = Identifier.fromNamespaceAndPath(block.getNamespace(), "textures/block/" + block.getPath() + ".png");
            return expected.toString().equals(resolvedTexture);
        } catch (RuntimeException ignored) {
            return false;
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

    private enum PickerTarget {
        TOOL("tool"),
        MATERIAL("material"),
        TEXTURE("fallback texture");

        private final String label;

        PickerTarget(String label) {
            this.label = label;
        }
    }

    private record PickerEntry(Identifier id, ItemStack stack) {
    }
}
