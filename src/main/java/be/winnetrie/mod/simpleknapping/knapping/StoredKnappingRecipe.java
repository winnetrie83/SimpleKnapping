package be.winnetrie.mod.simpleknapping.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Disk-safe GUI recipe representation storing registry IDs instead of mutable Item instances. */
public record StoredKnappingRecipe(
        Identifier id,
        Identifier knappingType,
        List<String> pattern,
        Identifier material,
        int materialCost,
        Identifier resultItem,
        int resultCount
) {
    public static final Codec<StoredKnappingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(StoredKnappingRecipe::id),
            Identifier.CODEC.fieldOf("knapping_type").forGetter(StoredKnappingRecipe::knappingType),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(StoredKnappingRecipe::pattern),
            Identifier.CODEC.optionalFieldOf("material").forGetter(recipe -> Optional.ofNullable(recipe.material())),
            Codec.INT.optionalFieldOf("material_cost", 0).forGetter(StoredKnappingRecipe::materialCost),
            Identifier.CODEC.fieldOf("result_item").forGetter(StoredKnappingRecipe::resultItem),
            Codec.INT.optionalFieldOf("result_count", 1).forGetter(StoredKnappingRecipe::resultCount)
    ).apply(instance, (id, type, pattern, material, materialCost, result, resultCount) ->
            new StoredKnappingRecipe(id, type, pattern, material.orElse(null), materialCost, result, resultCount)));

    public StoredKnappingRecipe(Identifier id, Identifier knappingType, List<String> pattern,
                                Identifier resultItem, int resultCount) {
        this(id, knappingType, pattern, null, 0, resultItem, resultCount);
    }

    public StoredKnappingRecipe {
        pattern = List.copyOf(pattern);
        validatePattern(pattern);
        if (materialCost < 0 || materialCost > 99) {
            throw new IllegalArgumentException("Knapping material amount must be between 0 and 99");
        }
        if (material != null && materialCost == 0) materialCost = 1;
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
            recipeMaterial = BuiltInRegistries.ITEM.getValue(material);
            if (recipeMaterial == Items.AIR) {
                throw new IllegalStateException("Air cannot be used as knapping material");
            }
        }
        Item item = BuiltInRegistries.ITEM.getValue(resultItem);
        if (item == Items.AIR) throw new IllegalStateException("Air cannot be a knapping result");
        return new KnappingRecipe(id, knappingType, pattern.toArray(String[]::new),
                recipeMaterial, materialCost, item, resultCount);
    }

    public static StoredKnappingRecipe fromRuntimeRecipe(KnappingRecipe recipe) {
        Identifier materialId = recipe.material() == null
                ? null : BuiltInRegistries.ITEM.getKey(recipe.material());
        Identifier resultItemId = BuiltInRegistries.ITEM.getKey(recipe.resultItem());
        return new StoredKnappingRecipe(recipe.id(), recipe.knappingType(),
                Arrays.asList(recipe.pattern().clone()), materialId,
                recipe.materialCost(), resultItemId, recipe.resultCount());
    }

    public static void validatePattern(List<String> pattern) {
        KnappingPatternRules.validate(pattern.toArray(String[]::new));
    }
}
