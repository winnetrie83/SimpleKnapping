package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.SimpleTier;

/** Tool tiers used by the Minecraft 1.21.1 backport. */
@SuppressWarnings("null")
public final class ModToolMaterials {

    public static final TagKey<Item> NO_REPAIR = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "no_repair")
    );

    /**
     * Flint deliberately uses stone-level harvesting in the 1.21.1 branch.
     * 1.21.1 has no vanilla copper-tool progression tier, so the flint pickaxe
     * must be able to harvest iron ore and lead naturally into iron equipment.
     */
    public static final TagKey<Block> INCORRECT_FOR_FLINT_TOOL = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "incorrect_for_flint_tool")
    );

    public static final Tier FLINT = new SimpleTier(
            INCORRECT_FOR_FLINT_TOOL,
            96,
            4.0F,
            1.0F,
            8,
            () -> Ingredient.of(NO_REPAIR)
    );

    /** Same combat profile as flint, but with the knife's lower durability. */
    public static final Tier FLINT_KNIFE = new SimpleTier(
            INCORRECT_FOR_FLINT_TOOL,
            64,
            4.0F,
            1.0F,
            8,
            () -> Ingredient.of(NO_REPAIR)
    );

    private ModToolMaterials() {
    }
}
