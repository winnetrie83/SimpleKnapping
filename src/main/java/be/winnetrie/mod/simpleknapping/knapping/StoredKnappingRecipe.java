package be.winnetrie.mod.simpleknapping.knapping;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.Arrays;
import java.util.List;

/**
 * Disk-safe representation of a GUI-created recipe.
 * Registry ids are stored instead of live Item instances.
 */
public record StoredKnappingRecipe(
        Identifier id,
        Identifier knappingType,
        List<String> pattern,
        Identifier resultItem,
        int resultCount
) {
    public static final Codec<StoredKnappingRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("id").forGetter(StoredKnappingRecipe::id),
            Identifier.CODEC.fieldOf("knapping_type").forGetter(StoredKnappingRecipe::knappingType),
            Codec.STRING.listOf().fieldOf("pattern").forGetter(StoredKnappingRecipe::pattern),
            Identifier.CODEC.fieldOf("result_item").forGetter(StoredKnappingRecipe::resultItem),
            Codec.INT.optionalFieldOf("result_count", 1).forGetter(StoredKnappingRecipe::resultCount)
    ).apply(instance, StoredKnappingRecipe::new));

    public StoredKnappingRecipe {
        pattern = List.copyOf(pattern);
        validatePattern(pattern);

        if (resultCount < 1 || resultCount > 99) {
            throw new IllegalArgumentException("Knapping result count must be between 1 and 99");
        }
    }

    public KnappingRecipe toRuntimeRecipe() {
        if (!BuiltInRegistries.ITEM.containsKey(resultItem)) {
            throw new IllegalStateException("Unknown knapping result item: " + resultItem);
        }

        Item item = BuiltInRegistries.ITEM.getValue(resultItem);
        return new KnappingRecipe(
                id,
                knappingType,
                pattern.toArray(String[]::new),
                item,
                resultCount
        );
    }

    public static StoredKnappingRecipe fromRuntimeRecipe(KnappingRecipe recipe) {
        Identifier resultItemId = BuiltInRegistries.ITEM.getKey(recipe.resultItem());
        return new StoredKnappingRecipe(
                recipe.id(),
                recipe.knappingType(),
                Arrays.asList(recipe.pattern().clone()),
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
