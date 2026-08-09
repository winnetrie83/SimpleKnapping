package be.winnetrie.mod.simpleknapping.knapping;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

/**
 * Runtime definition of one knapping family.
 *
 * material/materialCost are defaults for legacy recipes and for initializing
 * new recipes in the admin editor. Recipes may override both values.
 * Resource/datapack types may keep a legacy direct texture. GUI-created types
 * carry the block id selected by the admin as textureBlock. That configured
 * texture is a fallback: when the recipe material is itself a block item, the
 * client first tries that material block's texture automatically.
 */
public record KnappingType(
        Identifier id,
        Item tool,
        Item material,
        int materialCost,
        Identifier texture,
        Identifier textureBlock
) {
}
