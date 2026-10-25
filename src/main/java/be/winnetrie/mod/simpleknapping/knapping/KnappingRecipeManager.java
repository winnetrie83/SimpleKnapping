package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
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

/** Datapack recipes, world-local overrides and disabled recipes are kept as separate layers. */
public class KnappingRecipeManager extends SimpleJsonResourceReloadListener<JsonElement> {
    private static final Codec<JsonElement> JSON_CODEC = Codec.PASSTHROUGH.xmap(
            dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
            json -> new Dynamic<>(JsonOps.INSTANCE, json)
    );

    private static final Map<Identifier, KnappingRecipe> RESOURCE_RECIPES = new LinkedHashMap<>();
    private static final Map<Identifier, KnappingRecipe> SERVER_RECIPES = new LinkedHashMap<>();
    private static final Set<Identifier> DISABLED_RECIPES = new LinkedHashSet<>();
    public static final Map<Identifier, KnappingRecipe> RECIPES = new LinkedHashMap<>();

    public KnappingRecipeManager() {
        super(JSON_CODEC, FileToIdConverter.json("knapping_recipes"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap,
                         ResourceManager resourceManager, ProfilerFiller profiler) {
        RESOURCE_RECIPES.clear();
        for (Map.Entry<Identifier, JsonElement> entry : jsonMap.entrySet()) {
            try {
                KnappingRecipe recipe = parseResourceRecipe(entry.getKey(), entry.getValue().getAsJsonObject());
                RESOURCE_RECIPES.put(entry.getKey(), recipe);
            } catch (RuntimeException invalid) {
                // A malformed community datapack recipe must not prevent every valid recipe from loading.
                SimpleKnapping.LOGGER.error("Skipping invalid knapping recipe {}: {}",
                        entry.getKey(), invalid.getMessage());
            }
        }
        rebuildEffectiveRecipes();
    }

    private static KnappingRecipe parseResourceRecipe(Identifier id, JsonObject json) {
        if (json == null) throw new IllegalArgumentException("Recipe is not an object");
        Identifier type = Identifier.parse(json.get("knapping_type").getAsString());
        JsonArray values = json.getAsJsonArray("pattern");
        if (values == null) throw new IllegalArgumentException("Missing pattern");
        String[] pattern = new String[values.size()];
        for (int i = 0; i < pattern.length; i++) pattern[i] = values.get(i).getAsString();
        KnappingPatternRules.validate(pattern);

        Item material = null;
        int materialCost = 0;
        if (json.has("material")) {
            Identifier materialId = Identifier.parse(json.get("material").getAsString());
            material = checkedItem(materialId, "material");
            materialCost = json.has("material_cost") ? json.get("material_cost").getAsInt() : 1;
            if (materialCost < 1 || materialCost > 99) {
                throw new IllegalArgumentException("Explicit material cost must be between 1 and 99");
            }
        } else if (json.has("material_cost")) {
            materialCost = json.get("material_cost").getAsInt();
            if (materialCost < 1 || materialCost > 99) {
                throw new IllegalArgumentException("Material cost must be between 1 and 99");
            }
        }

        JsonObject result = json.getAsJsonObject("result");
        if (result == null) throw new IllegalArgumentException("Missing result object");
        Item resultItem = checkedItem(Identifier.parse(result.get("item").getAsString()), "result");
        int resultCount = result.has("count") ? result.get("count").getAsInt() : 1;
        if (resultCount < 1 || resultCount > 99) {
            throw new IllegalArgumentException("Result count must be between 1 and 99");
        }
        return new KnappingRecipe(id, type, pattern, material, materialCost, resultItem, resultCount);
    }

    private static Item checkedItem(Identifier id, String role) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) throw new IllegalArgumentException("Unknown " + role + " item " + id);
        Item item = BuiltInRegistries.ITEM.getValue(id);
        if (item == Items.AIR) throw new IllegalArgumentException("Air is not a usable " + role + " item");
        return item;
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
            if (!DISABLED_RECIPES.contains(entry.getKey())) RECIPES.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Identifier, KnappingRecipe> entry : SERVER_RECIPES.entrySet()) {
            if (!DISABLED_RECIPES.contains(entry.getKey())) RECIPES.put(entry.getKey(), entry.getValue());
        }
    }

    public static Map<Identifier, KnappingRecipe> getResourceRecipes() { return Collections.unmodifiableMap(RESOURCE_RECIPES); }
    public static Map<Identifier, KnappingRecipe> getServerRecipes() { return Collections.unmodifiableMap(SERVER_RECIPES); }
    public static Map<Identifier, KnappingRecipe> getEffectiveRecipes() { return Collections.unmodifiableMap(RECIPES); }
    public static Set<Identifier> getDisabledRecipes() { return Collections.unmodifiableSet(DISABLED_RECIPES); }

    public static Item resolveMaterial(KnappingRecipe recipe) {
        if (recipe.material() != null && recipe.material() != Items.AIR) return recipe.material();
        KnappingType type = KnappingTypeManager.getAnyLayer(recipe.knappingType());
        return type == null ? Items.AIR : type.material();
    }

    public static int resolveMaterialCost(KnappingRecipe recipe) {
        if (recipe.materialCost() > 0) return recipe.materialCost();
        KnappingType type = KnappingTypeManager.getAnyLayer(recipe.knappingType());
        return type == null ? 1 : type.materialCost();
    }

    public static boolean hasActiveRecipeForMaterial(Identifier type, Item material) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (recipe.knappingType().equals(type) && resolveMaterial(recipe) == material) return true;
        }
        return false;
    }

    public static int minimumMaterialCost(Identifier type, Item material) {
        int minimum = Integer.MAX_VALUE;
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (recipe.knappingType().equals(type) && resolveMaterial(recipe) == material) {
                minimum = Math.min(minimum, resolveMaterialCost(recipe));
            }
        }
        return minimum == Integer.MAX_VALUE ? 0 : minimum;
    }

    /** Legacy lookup without material filtering retained for existing callers. */
    public static KnappingRecipe findMatch(Identifier type, String[] pattern) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (recipe.knappingType().equals(type) && KnappingPatternRules.matches(recipe.pattern(), pattern)) return recipe;
        }
        return null;
    }

    public static KnappingRecipe findMatch(Identifier type, Item material, String[] pattern) {
        for (KnappingRecipe recipe : RECIPES.values()) {
            if (recipe.knappingType().equals(type) && resolveMaterial(recipe) == material
                    && KnappingPatternRules.matches(recipe.pattern(), pattern)) return recipe;
        }
        return null;
    }
}
