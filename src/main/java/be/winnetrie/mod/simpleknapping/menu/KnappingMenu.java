package be.winnetrie.mod.simpleknapping.menu;

import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
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

@SuppressWarnings("null")
public class KnappingMenu extends AbstractContainerMenu {

    public static final int GRID_SIZE = 5;
    public static final int TILE_COUNT = GRID_SIZE * GRID_SIZE;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 104;
    private static final int HOTBAR_Y = 162;

    private final int[] tiles = new int[TILE_COUNT];
    private final KnappingType knappingType;
    private final Item inputMaterial;
    private final ResultContainer resultContainer = new ResultContainer();
    private KnappingRecipe matchedRecipe = null;

    /** Legacy constructor; current opens pass the actual offhand material explicitly. */
    public KnappingMenu(int containerId, Inventory inventory, KnappingType knappingType) {
        this(containerId, inventory, knappingType, knappingType.material());
    }

    public KnappingMenu(int containerId, Inventory inventory, KnappingType knappingType, Item inputMaterial) {
        super(ModMenus.KNAPPING_MENU.get(), containerId);
        this.knappingType = knappingType;
        this.inputMaterial = inputMaterial;
        addKnappingDataSlots();
        addResultSlot();
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
    }

    public KnappingType getKnappingType() {
        return knappingType;
    }

    /** Material stack/item that opened this knapping session. */
    public Item getInputMaterial() {
        return inputMaterial;
    }

    private void addKnappingDataSlots() {
        for (int i = 0; i < TILE_COUNT; i++) {
            tiles[i] = 1;
            final int index = i;
            this.addDataSlot(new DataSlot() {
                @Override
                public int get() {
                    return tiles[index];
                }

                @Override
                public void set(int value) {
                    tiles[index] = value;
                }
            });
        }
    }

    private void addResultSlot() {
        this.addSlot(new Slot(resultContainer, 0, 128, 46) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return matchedRecipe != null && hasRequiredMaterial(player, matchedRecipe);
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (matchedRecipe == null || !consumeRequiredMaterial(player, matchedRecipe)) {
                    return;
                }

                super.onTake(player, stack);
                clearKnappingGrid();
                resultContainer.setItem(0, ItemStack.EMPTY);
                matchedRecipe = null;
                broadcastChanges();
            }
        });
    }

    private void addPlayerInventory(Inventory inventory) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(
                        inventory,
                        column + row * 9 + 9,
                        PLAYER_INVENTORY_X + column * 18,
                        PLAYER_INVENTORY_Y + row * 18
                ));
            }
        }
    }

    private void addPlayerHotbar(Inventory inventory) {
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(
                    inventory,
                    column,
                    PLAYER_INVENTORY_X + column * 18,
                    HOTBAR_Y
            ));
        }
    }

    public boolean hasTile(int index) {
        if (index < 0 || index >= TILE_COUNT) {
            return false;
        }
        return tiles[index] == 1;
    }

    public void removeTile(int index) {
        if (index < 0 || index >= TILE_COUNT || tiles[index] == 0) {
            return;
        }
        tiles[index] = 0;
        updateResult();
        broadcastChanges();
    }

    private void clearKnappingGrid() {
        for (int i = 0; i < TILE_COUNT; i++) {
            tiles[i] = 0;
        }
    }

    private void updateResult() {
        matchedRecipe = KnappingRecipeManager.findMatch(
                knappingType.id(),
                inputMaterial,
                getCurrentPattern()
        );

        if (matchedRecipe != null) {
            resultContainer.setItem(0, matchedRecipe.createResult());
        } else {
            resultContainer.setItem(0, ItemStack.EMPTY);
        }
    }

    private boolean hasRequiredMaterial(Player player, KnappingRecipe recipe) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        Item requiredMaterial = KnappingRecipeManager.resolveMaterial(recipe);
        int requiredAmount = KnappingRecipeManager.resolveMaterialCost(recipe);
        ItemStack offhand = player.getOffhandItem();
        return requiredMaterial == inputMaterial
                && !offhand.isEmpty()
                && offhand.getItem() == requiredMaterial
                && offhand.getCount() >= requiredAmount;
    }

    private boolean consumeRequiredMaterial(Player player, KnappingRecipe recipe) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        if (!hasRequiredMaterial(player, recipe)) {
            return false;
        }

        int amount = KnappingRecipeManager.resolveMaterialCost(recipe);
        ItemStack offhand = player.getOffhandItem();
        offhand.shrink(amount);
        player.setItemInHand(InteractionHand.OFF_HAND, offhand.isEmpty() ? ItemStack.EMPTY : offhand);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        return true;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (buttonId < 0 || buttonId >= TILE_COUNT) {
            return false;
        }
        if (!hasTile(buttonId)) {
            return true;
        }

        removeTile(buttonId);
        damageKnappingTool(player);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public String[] getCurrentPattern() {
        String[] pattern = new String[GRID_SIZE];
        for (int row = 0; row < GRID_SIZE; row++) {
            StringBuilder line = new StringBuilder();
            for (int col = 0; col < GRID_SIZE; col++) {
                int index = row * GRID_SIZE + col;
                line.append(tiles[index] == 1 ? "X" : " ");
            }
            pattern[row] = line.toString();
        }
        return pattern;
    }

    private void damageKnappingTool(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }
        if (player.getRandom().nextFloat() > 0.33F) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.isEmpty() || knappingType == null || tool.getItem() != knappingType.tool()) {
            return;
        }

        // Non-damageable items are valid configured knapping tools; they simply
        // do not receive durability damage when a tile is removed.
        if (tool.getMaxDamage() <= 0) {
            return;
        }

        tool.setDamageValue(tool.getDamageValue() + 1);
        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
    }
}
