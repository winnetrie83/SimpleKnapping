package be.winnetrie.mod.simpleknapping.knapping;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One 5x5 knapping recipe.
 *
 * material/materialCost may be omitted by legacy datapack recipes. In that case
 * KnappingRecipeManager resolves them from the recipe's knapping type defaults.
 */
public record KnappingRecipe(
        Identifier id,
        Identifier knappingType,
        String[] pattern,
        Item material,
        int materialCost,
        Item resultItem,
        int resultCount
 ) {
    /** Backwards-compatible constructor for legacy code/datagen. */
    public KnappingRecipe(
            Identifier id,
            Identifier knappingType,
            String[] pattern,
            Item resultItem,
            int resultCount
    ) {
        this(id, knappingType, pattern, null, 0, resultItem, resultCount);
    }

    public ItemStack createResult() {
        return new ItemStack(resultItem, resultCount);
    }
}
