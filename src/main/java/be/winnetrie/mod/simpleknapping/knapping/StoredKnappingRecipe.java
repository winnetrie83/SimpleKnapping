package be.winnetrie.mod.simpleknapping.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Disk-safe representation of a GUI-created recipe.
 * Registry ids are stored instead of live Item instances.
 *
 * material/material_cost are optional for backwards compatibility with
 * dev1/dev1.2 world data. Missing values inherit the knapping type defaults.
 */
@SuppressWarnings("null")
public record StoredKnappingRecipe(
        ResourceLocation id,
        ResourceLocation knappingType,
        List<String> pattern,
        ResourceLocation material,
        int materialCost,
        ResourceLocation resultItem,
        int resultCount
) {
    public static final Codec<StoredKnappingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(StoredKnappingRecipe::id),
            ResourceLocation.CODEC.fieldOf("knapping_type").forGetter(StoredKnappingRecipe::knappingType),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(StoredKnappingRecipe::pattern),
            ResourceLocation.CODEC.optionalFieldOf("material")
                    .forGetter(recipe -> Optional.ofNullable(recipe.material())),
            Codec.INT.optionalFieldOf("material_cost", 0).forGetter(StoredKnappingRecipe::materialCost),
            ResourceLocation.CODEC.fieldOf("result_item").forGetter(StoredKnappingRecipe::resultItem),
            Codec.INT.optionalFieldOf("result_count", 1).forGetter(StoredKnappingRecipe::resultCount)
    ).apply(instance, (id, type, pattern, material, materialCost, result, resultCount) ->
            new StoredKnappingRecipe(id, type, pattern, material.orElse(null), materialCost, result, resultCount)));

    /** Backwards-compatible constructor for pre-dev1.3 call sites. */
    public StoredKnappingRecipe(
            ResourceLocation id,
            ResourceLocation knappingType,
            List<String> pattern,
            ResourceLocation resultItem,
            int resultCount
    ) {
        this(id, knappingType, pattern, null, 0, resultItem, resultCount);
    }

    public StoredKnappingRecipe {
        pattern = List.copyOf(pattern);
        validatePattern(pattern);

        if (materialCost < 0 || materialCost > 99) {
            throw new IllegalArgumentException("Knapping material amount must be between 0 and 99");
        }
        if (material != null && materialCost == 0) {
            materialCost = 1;
        }
        if (resultCount < 1 || resultCount > 99) {
            throw new IllegalArgumentException("Knapping result count must be between 1 and 99");
        }
    }

    public KnappingRecipe toRuntimeRecipe() {
        if (!BuiltInRegistries.ITEM.containsKey(resultItem)) {
            throw new IllegalStateException("Unknown knapping result item: " + resultItem);
        }

        Item recipeMaterial = null;
        if (material != null) {
            if (!BuiltInRegistries.ITEM.containsKey(material)) {
                throw new IllegalStateException("Unknown knapping material item: " + material);
            }
            recipeMaterial = BuiltInRegistries.ITEM.get(material);
            if (recipeMaterial == Items.AIR) {
                throw new IllegalStateException("Air cannot be used as knapping material");
            }
        }

        Item item = BuiltInRegistries.ITEM.get(resultItem);
        return new KnappingRecipe(
                id,
                knappingType,
                pattern.toArray(String[]::new),
                recipeMaterial,
                materialCost,
                item,
                resultCount
        );
    }

    public static StoredKnappingRecipe fromRuntimeRecipe(KnappingRecipe recipe) {
        ResourceLocation materialId = recipe.material() == null
                ? null
                : BuiltInRegistries.ITEM.getKey(recipe.material());
        ResourceLocation resultItemId = BuiltInRegistries.ITEM.getKey(recipe.resultItem());
        return new StoredKnappingRecipe(
                recipe.id(),
                recipe.knappingType(),
                Arrays.asList(recipe.pattern().clone()),
                materialId,
                recipe.materialCost(),
                resultItemId,
                recipe.resultCount()
        );
    }

    public static void validatePattern(List<String> pattern) {
        if (pattern.size() != 5) {
            throw new IllegalArgumentException("A knapping pattern must contain exactly 5 rows");
        }

        for (String row : pattern) {
            if (row.length() != 5) {
                throw new IllegalArgumentException("Every knapping pattern row must contain exactly 5 characters");
            }

            for (int i = 0; i < row.length(); i++) {
                char value = row.charAt(i);
                if (value != 'X' && value != ' ') {
                    throw new IllegalArgumentException("Knapping patterns may only contain 'X' and spaces");
                }
            }
        }
    }
}
