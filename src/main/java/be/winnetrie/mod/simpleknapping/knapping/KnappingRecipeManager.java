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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Loads knapping recipes from resources and combines them with world-persistent
 * server recipes/overrides.
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

    /**
     * Effective runtime recipes. Kept public for compatibility with the current
     * SimpleKnapping code, but mutations should go through this manager.
     */
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

        JsonObject result = json.getAsJsonObject("result");
        Identifier resultItemId = Identifier.parse(result.get("item").getAsString());
        Item resultItem = BuiltInRegistries.ITEM.getValue(resultItemId);
        int resultCount = result.has("count") ? result.get("count").getAsInt() : 1;

        return new KnappingRecipe(id, knappingType, pattern, resultItem, resultCount);
    }

    /**
     * Replaces the complete world/server layer. This is used after world data is
     * loaded and after an admin saves/removes a recipe through the future GUI.
     */
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
                // Same id intentionally replaces the datapack/built-in recipe.
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

    public static KnappingRecipe findMatch(Identifier knappingType, String[] currentPattern) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (!recipe.knappingType().equals(knappingType)) {
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
