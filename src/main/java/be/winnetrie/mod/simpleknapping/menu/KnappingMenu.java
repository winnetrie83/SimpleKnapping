package be.winnetrie.mod.simpleknapping.menu;

import be.winnetrie.mod.simpleknapping.Config;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.registry.ModMenus;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Comparator;
import java.util.List;

/** All gameplay mutations and recipe previews are authoritative on the server. */
public class KnappingMenu extends AbstractContainerMenu {
    public static final int GRID_SIZE = 5;
    public static final int TILE_COUNT = GRID_SIZE * GRID_SIZE;
    public static final int RESET_BUTTON = TILE_COUNT;
    public static final int PREVIOUS_BUTTON = TILE_COUNT + 1;
    public static final int NEXT_BUTTON = TILE_COUNT + 2;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 104;
    private static final int HOTBAR_Y = 162;

    private final int[] tiles = new int[TILE_COUNT];
    private final int[] previewTiles = new int[TILE_COUNT];
    private final int[] previewMeta = new int[3]; // selected index, count, material cost
    private final int[] previewAccess = new int[1]; // server-authorized hint visibility
    private final KnappingType knappingType;
    private final Item inputMaterial;
    private final ResultContainer resultContainer = new ResultContainer();
    private KnappingRecipe matchedRecipe;

    /** Retained for call-site compatibility. */
    public KnappingMenu(int containerId, Inventory inventory, KnappingType knappingType) {
        this(containerId, inventory, knappingType, knappingType.material());
    }

    public KnappingMenu(int containerId, Inventory inventory, KnappingType knappingType, Item inputMaterial) {
        super(ModMenus.KNAPPING_MENU.get(), containerId);
        this.knappingType = knappingType;
        this.inputMaterial = inputMaterial;
        for (int i = 0; i < TILE_COUNT; i++) {
            tiles[i] = 1;
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() { return tiles[index]; }
                @Override public void set(int value) { tiles[index] = value; }
            });
        }
        for (int i = 0; i < TILE_COUNT; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() { return previewTiles[index]; }
                @Override public void set(int value) { previewTiles[index] = value; }
            });
        }
        for (int i = 0; i < previewMeta.length; i++) {
            final int index = i;
            addDataSlot(new DataSlot() {
                @Override public int get() { return previewMeta[index]; }
                @Override public void set(int value) { previewMeta[index] = value; }
            });
        }
        addDataSlot(new DataSlot() {
            @Override public int get() { return previewAccess[0]; }
            @Override public void set(int value) { previewAccess[0] = value; }
        });
        addResultSlot();
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
        // The server authorizes and populates previews after constructing this menu.
        // Never calculate/send patterns for an unauthorized player.
    }

    public KnappingType getKnappingType() { return knappingType; }
    public Item getInputMaterial() { return inputMaterial; }
    public boolean hasTile(int index) { return index >= 0 && index < TILE_COUNT && tiles[index] == 1; }
    public boolean hasPreviewTile(int index) { return index >= 0 && index < TILE_COUNT && previewTiles[index] == 1; }
    public int previewIndex() { return previewMeta[0]; }
    public int previewCount() { return previewMeta[1]; }
    public int previewCost() { return previewMeta[2]; }
    /** Synced to the client; the server, not the client config, controls access. */
    public boolean canViewRecipePreview() { return previewAccess[0] == 1; }

    /** Initialize/recheck permissions only on the logical server. */
    public void refreshPreviewPermission(Player player) {
        if (player.level().isClientSide()) return;
        boolean allowed = KnappingAccessPolicy.mayPreview(player.isCreative(),
                Config.ENABLE_SURVIVAL_RECIPE_PREVIEW.get());
        int flag = allowed ? 1 : 0;
        if (previewAccess[0] == flag) return;
        previewAccess[0] = flag;
        if (allowed) refreshPreview();
        else clearPreview();
        broadcastChanges();
    }

    private void clearPreview() {
        previewMeta[0] = 0;
        previewMeta[1] = 0;
        previewMeta[2] = 0;
        for (int i = 0; i < TILE_COUNT; i++) previewTiles[i] = 0;
    }

    private void addResultSlot() {
        addSlot(new Slot(resultContainer, 0, 128, 46) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }

            @Override public boolean mayPickup(Player player) {
                // The client does not receive the world's server-side recipe map.
                // The server remains the only authority on material consumption.
                if (player.level().isClientSide()) return !getItem().isEmpty();
                return stillValid(player) && validRecipeNow()
                        && hasRequiredMaterial(player, matchedRecipe);
            }

            @Override public void onTake(Player player, ItemStack stack) {
                if (player.level().isClientSide()) {
                    super.onTake(player, stack);
                    return;
                }
                // Re-check the recipe and ingredients immediately before payment.
                if (!stillValid(player) || !validRecipeNow()
                        || !consumeRequiredMaterial(player, matchedRecipe)) return;
                super.onTake(player, stack);
                resetGrid();
                broadcastChanges();
            }
        });
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18, PLAYER_INVENTORY_Y + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, PLAYER_INVENTORY_X + column * 18, HOTBAR_Y));
        }
    }

    private void removeTile(int index) {
        if (!hasTile(index)) return;
        tiles[index] = 0;
        updateResult();
        broadcastChanges();
    }

    private void resetGrid() {
        for (int i = 0; i < TILE_COUNT; i++) tiles[i] = 1;
        matchedRecipe = null;
        resultContainer.setItem(0, ItemStack.EMPTY);
    }

    private void updateResult() {
        matchedRecipe = KnappingRecipeManager.findMatch(knappingType.id(), inputMaterial, getCurrentPattern());
        resultContainer.setItem(0, matchedRecipe == null ? ItemStack.EMPTY : matchedRecipe.createResult());
    }

    private boolean validRecipeNow() {
        if (matchedRecipe == null) return false;
        KnappingRecipe live = KnappingRecipeManager.findMatch(
                knappingType.id(), inputMaterial, getCurrentPattern());
        // Reject a stale output even if an admin replaced the same recipe ID mid-session.
        return live != null && live.id().equals(matchedRecipe.id())
                && live.resultItem() == matchedRecipe.resultItem()
                && live.resultCount() == matchedRecipe.resultCount()
                && KnappingRecipeManager.resolveMaterialCost(live)
                    == KnappingRecipeManager.resolveMaterialCost(matchedRecipe);
    }

    private boolean hasRequiredMaterial(Player player, KnappingRecipe recipe) {
        if (recipe == null) return false;
        if (player.hasInfiniteMaterials()) return true;
        Item required = KnappingRecipeManager.resolveMaterial(recipe);
        int amount = KnappingRecipeManager.resolveMaterialCost(recipe);
        ItemStack offhand = player.getOffhandItem();
        return required == inputMaterial && !offhand.isEmpty()
                && offhand.getItem() == required && offhand.getCount() >= amount;
    }

    private boolean consumeRequiredMaterial(Player player, KnappingRecipe recipe) {
        if (recipe == null) return false;
        if (player.hasInfiniteMaterials()) return true;
        if (!hasRequiredMaterial(player, recipe)) return false;
        ItemStack offhand = player.getOffhandItem();
        offhand.shrink(KnappingRecipeManager.resolveMaterialCost(recipe));
        player.setItemInHand(InteractionHand.OFF_HAND, offhand.isEmpty() ? ItemStack.EMPTY : offhand);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    private List<KnappingRecipe> availableRecipes() {
        return KnappingRecipeManager.RECIPES.values().stream()
                .filter(recipe -> recipe.knappingType().equals(knappingType.id()))
                .filter(recipe -> KnappingRecipeManager.resolveMaterial(recipe) == inputMaterial)
                .sorted(Comparator.comparing(recipe -> recipe.id().toString()))
                .toList();
    }

    private void refreshPreview() {
        if (!canViewRecipePreview()) {
            clearPreview();
            return;
        }
        List<KnappingRecipe> recipes = availableRecipes();
        previewMeta[1] = recipes.size();
        if (recipes.isEmpty()) {
            previewMeta[0] = 0;
            previewMeta[2] = 0;
            for (int i = 0; i < TILE_COUNT; i++) previewTiles[i] = 0;
            return;
        }
        previewMeta[0] = Math.floorMod(previewMeta[0], recipes.size());
        KnappingRecipe recipe = recipes.get(previewMeta[0]);
        previewMeta[2] = KnappingRecipeManager.resolveMaterialCost(recipe);
        String[] rows = recipe.pattern();
        for (int i = 0; i < TILE_COUNT; i++) {
            previewTiles[i] = i / GRID_SIZE < rows.length
                    && rows[i / GRID_SIZE].length() > i % GRID_SIZE
                    && rows[i / GRID_SIZE].charAt(i % GRID_SIZE) == 'X' ? 1 : 0;
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!stillValid(player)) return false;
        refreshPreviewPermission(player);
        // A final strike may have broken the tool: the completed output may still
        // be claimed, but no extra GUI actions are allowed until a tool is held.
        if (player.getMainHandItem().getItem() != knappingType.tool()) return false;
        if (buttonId == RESET_BUTTON) {
            if (!KnappingAccessPolicy.mayReset(player.isCreative())) return false;
            resetGrid();
            refreshPreview();
        } else if (buttonId == PREVIOUS_BUTTON || buttonId == NEXT_BUTTON) {
            if (!canViewRecipePreview()) return false;
            previewMeta[0] += buttonId == NEXT_BUTTON ? 1 : -1;
            refreshPreview();
        } else if (buttonId >= 0 && buttonId < TILE_COUNT) {
            if (hasTile(buttonId)) {
                // The server alone decides whether a tile is removed and whether a tool is damaged.
                if (player.getMainHandItem().getItem() != knappingType.tool()) return false;
                removeTile(buttonId);
                damageKnappingTool(player);
            }
        } else {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override public ItemStack quickMoveStack(Player player, int slotIndex) {
        // Deliberately disabled for the crafted output: no shift-click duplicate path.
        return ItemStack.EMPTY;
    }

    @Override public boolean stillValid(Player player) {
        // Client-side runtime recipe maps need not mirror world-persistent server types.
        if (player.level().isClientSide()) return true;
        if (!player.isAlive() || player.getOffhandItem().isEmpty()
                || player.getOffhandItem().getItem() != inputMaterial) return false;
        KnappingType liveType = KnappingTypeManager.get(knappingType.id());
        if (liveType == null || liveType.tool() != knappingType.tool()) return false;
        // After the last strike breaks the tool, the already-crafted result may still be taken.
        return player.getMainHandItem().getItem() == knappingType.tool()
                || validRecipeNow();
    }

    public String[] getCurrentPattern() {
        String[] result = new String[GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            StringBuilder builder = new StringBuilder(GRID_SIZE);
            for (int col = 0; col < GRID_SIZE; col++) {
                builder.append(hasTile(row * GRID_SIZE + col) ? 'X' : ' ');
            }
            result[row] = builder.toString();
        }
        return result;
    }

    private void damageKnappingTool(Player player) {
        if (player.hasInfiniteMaterials()) return;
        if (player.getRandom().nextDouble() >= Config.KNAPPING_TOOL_DAMAGE_CHANCE.get()) return;
        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || tool.getItem() != knappingType.tool() || tool.getMaxDamage() <= 0) return;
        tool.setDamageValue(tool.getDamageValue() + 1);
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
}
