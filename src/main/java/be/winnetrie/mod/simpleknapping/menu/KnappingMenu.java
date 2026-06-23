package be.winnetrie.mod.simpleknapping.menu;

import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.registry.ModMenus;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class KnappingMenu extends AbstractContainerMenu {

    public static final int GRID_SIZE = 5;
    public static final int TILE_COUNT = GRID_SIZE * GRID_SIZE;

    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 104;
    private static final int HOTBAR_Y = 162;

    private final int[] tiles = new int[TILE_COUNT];
    private final KnappingType knappingType;
    private final ResultContainer resultContainer = new ResultContainer();

    private KnappingRecipe matchedRecipe = null;

    public KnappingMenu(int containerId, Inventory inventory, KnappingType knappingType) {
        super(ModMenus.KNAPPING_MENU.get(), containerId);

        this.knappingType = knappingType;

        addKnappingDataSlots();
        addResultSlot();
        addPlayerInventory(inventory);
        addPlayerHotbar(inventory);
    }

    public KnappingType getKnappingType() {
        return knappingType;
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
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);

                System.out.println("Player took knapping result");
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

        if (index < 0 || index >= TILE_COUNT) {
            return;
        }

        tiles[index] = 0;
        this.broadcastChanges();
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {

        if (buttonId < 0 || buttonId >= TILE_COUNT) {
            return false;
        }

        removeTile(buttonId);

        damageKnappingTool(player);

        matchedRecipe = KnappingRecipeManager.findMatch(
                knappingType.id(),
                getCurrentPattern()
        );

        if (matchedRecipe != null) {

            resultContainer.setItem(0, matchedRecipe.createResult());
            broadcastChanges();

            System.out.println("Matched recipe: " + matchedRecipe.id());

        } else {

            resultContainer.setItem(0, ItemStack.EMPTY);
            broadcastChanges();
        }

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

                if (tiles[index] == 1) {
                    line.append("X");
                } else {
                    line.append(" ");
                }
            }

            pattern[row] = line.toString();
        }

        return pattern;
    }

    private void damageKnappingTool(Player player) {

        if (player.hasInfiniteMaterials()) {
            return;
        }

        if (player.getRandom().nextFloat() > 0.33F) {
            return;
        }

        ItemStack tool = player.getMainHandItem();

        if (tool.isEmpty()) {
            return;
        }

        tool.setDamageValue(tool.getDamageValue() + 1);

        if (tool.getDamageValue() >= tool.getMaxDamage()) {
            player.setItemInHand(player.getUsedItemHand(), ItemStack.EMPTY);
        }
    }
}