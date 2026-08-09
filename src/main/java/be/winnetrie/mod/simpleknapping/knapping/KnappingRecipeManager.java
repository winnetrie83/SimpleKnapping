package be.winnetrie.mod.simpleknapping.knapping;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads knapping recipes from resources and combines them with world-persistent
 * server recipes/overrides.
 *
 * Recipe-level material support:
 *  - "material" and "material_cost" can be defined directly on a recipe.
 *  - legacy recipes that omit them inherit the defaults from their knapping type.
 *  - the same 5x5 pattern may therefore be reused for different materials.
 *
 * Merge priority:
 *  1. datapack / built-in resource recipes
 *  2. server recipes (same id = override)
 *  3. disabled ids are removed from the effective recipe set
 */
public class KnappingRecipeManager extends SimpleJsonResourceReloadListener<JsonElement> {

    private static final Codec<JsonElement> JSON_CODEC =
            Codec.PASSTHROUGH.xmap(
                    dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
                    json -> new Dynamic<>(JsonOps.INSTANCE, json)
            );

    private static final Map<Identifier, KnappingRecipe> RESOURCE_RECIPES = new LinkedHashMap<>();
    private static final Map<Identifier, KnappingRecipe> SERVER_RECIPES = new LinkedHashMap<>();
    private static final Set<Identifier> DISABLED_RECIPES = new LinkedHashSet<>();

    /** Effective runtime recipes. */
    public static final Map<Identifier, KnappingRecipe> RECIPES = new LinkedHashMap<>();

    public KnappingRecipeManager() {
        super(JSON_CODEC, FileToIdConverter.json("knapping_recipes"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap,
                         ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        RESOURCE_RECIPES.clear();

        for (Map.Entry<Identifier, JsonElement> entry : jsonMap.entrySet()) {
            Identifier id = entry.getKey();
            KnappingRecipe recipe = parseResourceRecipe(id, entry.getValue().getAsJsonObject());
            RESOURCE_RECIPES.put(id, recipe);
        }

        rebuildEffectiveRecipes();
    }

    private static KnappingRecipe parseResourceRecipe(Identifier id, JsonObject json) {
        Identifier knappingType = Identifier.parse(json.get("knapping_type").getAsString());

        JsonArray patternArray = json.getAsJsonArray("pattern");
        String[] pattern = new String[patternArray.size()];
        for (int i = 0; i < patternArray.size(); i++) {
            pattern[i] = patternArray.get(i).getAsString();
        }

        Item material = null;
        int materialCost = 0;
        if (json.has("material")) {
            Identifier materialId = Identifier.parse(json.get("material").getAsString());
            if (!BuiltInRegistries.ITEM.containsKey(materialId)) {
                throw new IllegalArgumentException("Unknown knapping material item: " + materialId);
            }
            material = BuiltInRegistries.ITEM.getValue(materialId);
            if (material == Items.AIR) {
                throw new IllegalArgumentException("Air cannot be used as knapping material");
            }
            materialCost = json.has("material_cost") ? json.get("material_cost").getAsInt() : 1;
        } else if (json.has("material_cost")) {
            // Allows a datapack to keep the type's default material but override the amount.
            materialCost = json.get("material_cost").getAsInt();
        }

        if (materialCost < 0 || materialCost > 99) {
            throw new IllegalArgumentException("material_cost must be between 0 and 99");
        }

        JsonObject result = json.getAsJsonObject("result");
        Identifier resultItemId = Identifier.parse(result.get("item").getAsString());
        Item resultItem = BuiltInRegistries.ITEM.getValue(resultItemId);
        int resultCount = result.has("count") ? result.get("count").getAsInt() : 1;

        return new KnappingRecipe(
                id,
                knappingType,
                pattern,
                material,
                materialCost,
                resultItem,
                resultCount
        );
    }

    public static void setServerState(Map<Identifier, KnappingRecipe> serverRecipes,
                                      Set<Identifier> disabledRecipes) {
        SERVER_RECIPES.clear();
        SERVER_RECIPES.putAll(serverRecipes);

        DISABLED_RECIPES.clear();
        DISABLED_RECIPES.addAll(disabledRecipes);

        rebuildEffectiveRecipes();
    }

    private static void rebuildEffectiveRecipes() {
        RECIPES.clear();

        for (Map.Entry<Identifier, KnappingRecipe> entry : RESOURCE_RECIPES.entrySet()) {
            if (!DISABLED_RECIPES.contains(entry.getKey())) {
                RECIPES.put(entry.getKey(), entry.getValue());
            }
        }

        for (Map.Entry<Identifier, KnappingRecipe> entry : SERVER_RECIPES.entrySet()) {
            if (!DISABLED_RECIPES.contains(entry.getKey())) {
                RECIPES.put(entry.getKey(), entry.getValue());
            }
        }
    }

    public static Map<Identifier, KnappingRecipe> getResourceRecipes() {
        return Collections.unmodifiableMap(RESOURCE_RECIPES);
    }

    public static Map<Identifier, KnappingRecipe> getServerRecipes() {
        return Collections.unmodifiableMap(SERVER_RECIPES);
    }

    public static Map<Identifier, KnappingRecipe> getEffectiveRecipes() {
        return Collections.unmodifiableMap(RECIPES);
    }

    public static Set<Identifier> getDisabledRecipes() {
        return Collections.unmodifiableSet(DISABLED_RECIPES);
    }

    /** Resolve a recipe's actual material, including legacy type fallback. */
    public static Item resolveMaterial(KnappingRecipe recipe) {
        if (recipe.material() != null && recipe.material() != Items.AIR) {
            return recipe.material();
        }

        KnappingType type = KnappingTypeManager.getAnyLayer(recipe.knappingType());
        return type == null ? Items.AIR : type.material();
    }

    /** Resolve a recipe's actual material amount, including legacy type fallback. */
    public static int resolveMaterialCost(KnappingRecipe recipe) {
        if (recipe.materialCost() > 0) {
            return recipe.materialCost();
        }

        KnappingType type = KnappingTypeManager.getAnyLayer(recipe.knappingType());
        return type == null ? 1 : type.materialCost();
    }

    public static boolean hasActiveRecipeForMaterial(Identifier knappingType, Item material) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (recipe.knappingType().equals(knappingType) && resolveMaterial(recipe) == material) {
                return true;
            }
        }
        return false;
    }

    /**
     * Smallest amount needed by any active recipe in a type for this material.
     * Returns 0 if that type has no active recipe for the material.
     */
    public static int minimumMaterialCost(Identifier knappingType, Item material) {
        int minimum = Integer.MAX_VALUE;
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (!recipe.knappingType().equals(knappingType) || resolveMaterial(recipe) != material) {
                continue;
            }
            minimum = Math.min(minimum, resolveMaterialCost(recipe));
        }
        return minimum == Integer.MAX_VALUE ? 0 : minimum;
    }

    /** Legacy overload retained for compatibility. */
    public static KnappingRecipe findMatch(Identifier knappingType, String[] currentPattern) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (recipe.knappingType().equals(knappingType) && matches(recipe.pattern(), currentPattern)) {
                return recipe;
            }
        }
        return null;
    }

    /** Match on type + input material + 5x5 pattern. */
    public static KnappingRecipe findMatch(Identifier knappingType, Item material, String[] currentPattern) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (!recipe.knappingType().equals(knappingType)) {
                continue;
            }
            if (resolveMaterial(recipe) != material) {
                continue;
            }
            if (matches(recipe.pattern(), currentPattern)) {
                return recipe;
            }
        }
        return null;
    }

    private static boolean matches(String[] recipePattern, String[] currentPattern) {
        if (recipePattern.length != currentPattern.length) {
            return false;
        }

        for (int row = 0; row < recipePattern.length; row++) {
            if (!recipePattern[row].equals(currentPattern[row])) {
                return false;
            }
        }

        return true;
    }
}
