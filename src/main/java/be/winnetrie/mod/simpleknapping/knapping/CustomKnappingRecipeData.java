package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** World-persistent server data used by the custom recipe editor. */
@SuppressWarnings("null")
public final class CustomKnappingRecipeData extends SavedData {
    private static final String DATA_NAME = "simpleknapping_custom_knapping_recipes";
    private static final SavedData.Factory<CustomKnappingRecipeData> FACTORY =
            new SavedData.Factory<>(CustomKnappingRecipeData::new, CustomKnappingRecipeData::load);

    private final Map<ResourceLocation, StoredKnappingRecipe> recipes = new LinkedHashMap<>();
    private final Set<ResourceLocation> disabled = new LinkedHashSet<>();

    public CustomKnappingRecipeData() {
    }

    public static CustomKnappingRecipeData get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not available");
        }
        CustomKnappingRecipeData data = server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
        data.syncRuntimeState();
        return data;
    }

    private static CustomKnappingRecipeData load(CompoundTag tag, HolderLookup.Provider registries) {
        CustomKnappingRecipeData data = new CustomKnappingRecipeData();

        ListTag recipeTags = tag.getList("recipes", Tag.TAG_COMPOUND);
        for (int i = 0; i < recipeTags.size(); i++) {
            try {
                CompoundTag recipeTag = recipeTags.getCompound(i);
                ResourceLocation id = ResourceLocation.parse(recipeTag.getString("id"));
                ResourceLocation type = ResourceLocation.parse(recipeTag.getString("knapping_type"));

                List<String> pattern = new ArrayList<>(5);
                ListTag patternTags = recipeTag.getList("pattern", Tag.TAG_STRING);
                for (int row = 0; row < patternTags.size(); row++) {
                    pattern.add(patternTags.getString(row));
                }

                ResourceLocation material = recipeTag.contains("material", Tag.TAG_STRING)
                        ? ResourceLocation.parse(recipeTag.getString("material"))
                        : null;
                int materialCost = recipeTag.contains("material_cost", Tag.TAG_INT)
                        ? recipeTag.getInt("material_cost")
                        : 0;
                ResourceLocation result = ResourceLocation.parse(recipeTag.getString("result_item"));
                int resultCount = recipeTag.contains("result_count", Tag.TAG_INT)
                        ? recipeTag.getInt("result_count")
                        : 1;

                StoredKnappingRecipe recipe = new StoredKnappingRecipe(
                        id, type, pattern, material, materialCost, result, resultCount
                );
                data.recipes.put(id, recipe);
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.warn("Skipping invalid stored knapping recipe entry {}", i, exception);
            }
        }

        ListTag disabledTags = tag.getList("disabled", Tag.TAG_STRING);
        for (int i = 0; i < disabledTags.size(); i++) {
            try {
                data.disabled.add(ResourceLocation.parse(disabledTags.getString(i)));
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.warn("Skipping invalid disabled knapping recipe id at index {}", i, exception);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag recipeTags = new ListTag();
        for (StoredKnappingRecipe recipe : recipes.values()) {
            CompoundTag recipeTag = new CompoundTag();
            recipeTag.putString("id", recipe.id().toString());
            recipeTag.putString("knapping_type", recipe.knappingType().toString());

            ListTag patternTags = new ListTag();
            for (String row : recipe.pattern()) {
                patternTags.add(StringTag.valueOf(row));
            }
            recipeTag.put("pattern", patternTags);

            if (recipe.material() != null) {
                recipeTag.putString("material", recipe.material().toString());
            }
            recipeTag.putInt("material_cost", recipe.materialCost());
            recipeTag.putString("result_item", recipe.resultItem().toString());
            recipeTag.putInt("result_count", recipe.resultCount());
            recipeTags.add(recipeTag);
        }
        tag.put("recipes", recipeTags);

        ListTag disabledTags = new ListTag();
        for (ResourceLocation id : disabled) {
            disabledTags.add(StringTag.valueOf(id.toString()));
        }
        tag.put("disabled", disabledTags);
        return tag;
    }

    public Collection<StoredKnappingRecipe> getRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public Set<ResourceLocation> getDisabledRecipes() {
        return Collections.unmodifiableSet(disabled);
    }

    public StoredKnappingRecipe getRecipe(ResourceLocation id) {
        return recipes.get(id);
    }

    public boolean hasOverride(ResourceLocation id) {
        return recipes.containsKey(id);
    }

    public void upsertRecipe(StoredKnappingRecipe recipe) {
        recipes.put(recipe.id(), recipe);
        disabled.remove(recipe.id());
        changed();
    }

    public boolean removeOverride(ResourceLocation id) {
        StoredKnappingRecipe removed = recipes.remove(id);
        if (removed != null) {
            changed();
            return true;
        }
        return false;
    }

    public void setRecipeDisabled(ResourceLocation id, boolean isDisabled) {
        boolean stateChanged = isDisabled ? disabled.add(id) : disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    public void restoreOriginal(ResourceLocation id) {
        boolean stateChanged = recipes.remove(id) != null;
        stateChanged |= disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    public void syncRuntimeState() {
        Map<ResourceLocation, KnappingRecipe> runtimeRecipes = new LinkedHashMap<>();
        for (StoredKnappingRecipe stored : recipes.values()) {
            try {
                runtimeRecipes.put(stored.id(), stored.toRuntimeRecipe());
            } catch (RuntimeException exception) {
                SimpleKnapping.LOGGER.error("Could not activate stored knapping recipe {}", stored.id(), exception);
            }
        }
        KnappingRecipeManager.setServerState(runtimeRecipes, disabled);
    }

    private void changed() {
        setDirty();
        syncRuntimeState();
    }
}
