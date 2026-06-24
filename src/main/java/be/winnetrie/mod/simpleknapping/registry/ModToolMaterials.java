package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

public class ModToolMaterials {

    public static final TagKey<Item> NO_REPAIR = TagKey.create(
            Registries.ITEM,
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "no_repair")
    );

    public static final TagKey<Block> INCORRECT_FOR_FLINT_TOOL = TagKey.create(
        Registries.BLOCK,
        Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "incorrect_for_flint_tool")
);

    public static final ToolMaterial FLINT = new ToolMaterial(
        INCORRECT_FOR_FLINT_TOOL,
        96,
        4.0F,
        1.0F,
        8,
        NO_REPAIR
    );
}