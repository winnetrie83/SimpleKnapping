package be.winnetrie.mod.simpleknapping.knapping;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
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

import java.util.HashMap;
import java.util.Map;

public class KnappingRecipeManager extends SimpleJsonResourceReloadListener<JsonElement> {

    private static final Codec<JsonElement> JSON_CODEC =
            Codec.PASSTHROUGH.xmap(
                    dynamic -> dynamic.convert(JsonOps.INSTANCE).getValue(),
                    json -> new Dynamic<>(JsonOps.INSTANCE, json)
            );

    public static final Map<Identifier, KnappingRecipe> RECIPES = new HashMap<>();

    public KnappingRecipeManager() {
        super(JSON_CODEC, FileToIdConverter.json("knapping_recipes"));
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> jsonMap, ResourceManager resourceManager, ProfilerFiller profiler) {

        RECIPES.clear();

        for (Map.Entry<Identifier, JsonElement> entry : jsonMap.entrySet()) {

            Identifier id = entry.getKey();
            JsonObject json = entry.getValue().getAsJsonObject();

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

            RECIPES.put(id, new KnappingRecipe(id, knappingType, pattern, resultItem, resultCount));

            System.out.println("Loaded knapping recipe: " + id);
        }
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