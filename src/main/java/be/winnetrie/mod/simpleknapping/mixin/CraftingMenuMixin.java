package be.winnetrie.mod.simpleknapping.mixin;

import be.winnetrie.mod.simpleknapping.progression.RecipeLockManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(CraftingMenu.class)
public class CraftingMenuMixin {

    @ModifyVariable(
            method = "slotChangedCraftingGrid",
            at = @At(
                    value = "STORE",
                    ordinal = 1
            ),
            ordinal = 0
    )
    private static ItemStack simpleknapping$lockCraftingResult(
            ItemStack result,
            AbstractContainerMenu menu,
            ServerLevel level,
            Player player,
            CraftingContainer container,
            ResultContainer resultSlots,
            RecipeHolder<CraftingRecipe> recipeHint
    ) {
        if (result.isEmpty()) {
            return result;
        }

        if (RecipeLockManager.isLocked(player, result)) {
            return ItemStack.EMPTY;
        }

        return result;
    }
}