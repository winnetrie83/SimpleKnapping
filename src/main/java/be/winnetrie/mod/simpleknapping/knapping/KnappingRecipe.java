package be.winnetrie.mod.simpleknapping.knapping;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public record KnappingRecipe(
        Identifier id,
        Identifier knappingType,
        String[] pattern,
        Item resultItem,
        int resultCount
) {

    public ItemStack createResult() {
        return new ItemStack(resultItem, resultCount);
    }
}