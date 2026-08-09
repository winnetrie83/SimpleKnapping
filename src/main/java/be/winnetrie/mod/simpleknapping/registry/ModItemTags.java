package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

@SuppressWarnings("null")
public class ModItemTags {

    public static final TagKey<Item> KNIVES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "knives")
    );

    public static final TagKey<Item> REMOVED_FROM_LOOT = TagKey.create(
        Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(SimpleKnapping.MODID, "removed_from_loot")
);
}