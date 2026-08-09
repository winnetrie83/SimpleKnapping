package be.winnetrie.mod.simpleknapping.guide;

import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import com.google.gson.Gson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Small player-facing snapshot containing only recipes that are currently
 * usable. It is generated fresh whenever /simpleknapping guide is opened, so
 * custom recipes, overrides and disabled recipes are reflected immediately.
 */
public record RecipeGuideSnapshot(List<Entry> recipes) {
    private static final Gson GSON = new Gson();

    public RecipeGuideSnapshot {
        recipes = recipes == null ? List.of() : List.copyOf(recipes);
    }

    public static RecipeGuideSnapshot fromServer() {
        List<Entry> entries = new ArrayList<>();

        for (Map.Entry<Identifier, KnappingRecipe> mapEntry : KnappingRecipeManager.getEffectiveRecipes().entrySet()) {
            Identifier recipeId = mapEntry.getKey();
            KnappingRecipe recipe = mapEntry.getValue();
            KnappingType type = KnappingTypeManager.get(recipe.knappingType());
            if (type == null) {
                // Type disabled or missing: the recipe cannot be started.
                continue;
            }

            if (DisabledVanillaEquipment.isDisabled(recipe.resultItem())) {
                // Do not advertise a result which the server currently treats
                // as unavailable/inert for survival progression.
                continue;
            }

            Item material = KnappingRecipeManager.resolveMaterial(recipe);
            if (material == null || material == Items.AIR) {
                continue;
            }

            Identifier materialId = BuiltInRegistries.ITEM.getKey(material);
            Identifier resultId = BuiltInRegistries.ITEM.getKey(recipe.resultItem());
            Identifier toolId = BuiltInRegistries.ITEM.getKey(type.tool());
            if (materialId == null || resultId == null || toolId == null) {
                continue;
            }

            entries.add(new Entry(
                    recipeId.toString(),
                    recipe.knappingType().toString(),
                    toolId.toString(),
                    materialId.toString(),
                    KnappingRecipeManager.resolveMaterialCost(recipe),
                    Arrays.asList(recipe.pattern().clone()),
                    resultId.toString(),
                    recipe.resultCount(),
                    type.texture().toString()
            ));
        }

        entries.sort(Comparator.comparing(Entry::resultItem).thenComparing(Entry::id));
        return new RecipeGuideSnapshot(entries);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static RecipeGuideSnapshot fromJson(String json) {
        RecipeGuideSnapshot snapshot = GSON.fromJson(json, RecipeGuideSnapshot.class);
        if (snapshot == null) {
            throw new IllegalArgumentException("Missing knapping recipe guide snapshot");
        }
        return new RecipeGuideSnapshot(snapshot.recipes);
    }

    public record Entry(
            String id,
            String knappingType,
            String tool,
            String material,
            int materialCost,
            List<String> pattern,
            String resultItem,
            int resultCount,
            String fallbackTexture
    ) {
        public Entry {
            materialCost = Math.max(1, materialCost);
            resultCount = Math.max(1, resultCount);
            pattern = pattern == null ? List.of() : List.copyOf(pattern);
            fallbackTexture = fallbackTexture == null
                    ? "minecraft:textures/block/clay.png"
                    : fallbackTexture;
        }
    }
}
