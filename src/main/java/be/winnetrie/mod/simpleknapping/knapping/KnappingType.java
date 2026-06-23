package be.winnetrie.mod.simpleknapping.knapping;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

public record KnappingType(
        Identifier id,
        Item material,
        int materialCost,
        Identifier texture
) {
}