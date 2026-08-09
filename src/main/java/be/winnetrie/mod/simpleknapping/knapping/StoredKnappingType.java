package be.winnetrie.mod.simpleknapping.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** Disk-safe representation of a GUI-created/overridden knapping type. */
public record StoredKnappingType(
        Identifier id,
        Identifier tool,
        Identifier material,
        int materialCost,
        Identifier textureBlock
) {
    public static final Codec<StoredKnappingType> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(StoredKnappingType::id),
            Identifier.CODEC.fieldOf("tool").forGetter(StoredKnappingType::tool),
            Identifier.CODEC.fieldOf("material").forGetter(StoredKnappingType::material),
            Codec.INT.optionalFieldOf("material_cost", 1).forGetter(StoredKnappingType::materialCost),
            Identifier.CODEC.optionalFieldOf("texture_block", KnappingTypeManager.DEFAULT_TEXTURE_BLOCK)
                    .forGetter(StoredKnappingType::textureBlock)
    ).apply(instance, StoredKnappingType::new));

    public StoredKnappingType {
        if (materialCost < 1 || materialCost > 99) {
            throw new IllegalArgumentException("Knapping material amount must be between 1 and 99");
        }
    }

    public KnappingType toRuntimeType() {
        if (!BuiltInRegistries.ITEM.containsKey(tool)) {
            throw new IllegalStateException("Unknown knapping tool item: " + tool);
        }
        if (!BuiltInRegistries.ITEM.containsKey(material)) {
            throw new IllegalStateException("Unknown knapping material item: " + material);
        }
        Item toolItem = BuiltInRegistries.ITEM.getValue(tool);
        Item materialItem = BuiltInRegistries.ITEM.getValue(material);
        if (toolItem == Items.AIR || materialItem == Items.AIR) {
            throw new IllegalStateException("Knapping tool/material cannot be air");
        }

        // Texture blocks are intentionally soft dependencies. If a mod that
        // supplied the selected block disappears, the type remains usable and
        // renders with the guaranteed vanilla clay fallback.
        Identifier safeTextureBlock = KnappingTypeManager.validTextureBlockOrDefault(textureBlock);

        return new KnappingType(
                id,
                toolItem,
                materialItem,
                materialCost,
                KnappingTypeManager.textureForBlock(safeTextureBlock),
                safeTextureBlock
        );
    }
}
