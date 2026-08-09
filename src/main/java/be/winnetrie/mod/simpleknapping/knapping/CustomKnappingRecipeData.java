package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * World-persistent server data used by the custom recipe editor.
 *
 * This deliberately stores only the GUI/admin layer. Datapack and built-in
 * recipes remain owned by the resource reload system and are never rewritten.
 */
public final class CustomKnappingRecipeData extends SavedData {

    public static final SavedDataType<CustomKnappingRecipeData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "custom_knapping_recipes"),
            CustomKnappingRecipeData::new,
            RecordCodecBuilder.create(instance -> instance.group(
                    StoredKnappingRecipe.CODEC.listOf()
                            .optionalFieldOf("recipes", List.of())
                            .forGetter(CustomKnappingRecipeData::recipesForCodec),
                    Identifier.CODEC.listOf()
                            .optionalFieldOf("disabled", List.of())
                            .forGetter(CustomKnappingRecipeData::disabledForCodec)
            ).apply(instance, CustomKnappingRecipeData::new)),
            null
    );

    private final Map<Identifier, StoredKnappingRecipe> recipes = new LinkedHashMap<>();
    private final Set<Identifier> disabled = new LinkedHashSet<>();

    public CustomKnappingRecipeData() {
    }

    private CustomKnappingRecipeData(List<StoredKnappingRecipe> recipes, List<Identifier> disabled) {
        for (StoredKnappingRecipe recipe : recipes) {
            this.recipes.put(recipe.id(), recipe);
        }
        this.disabled.addAll(disabled);
    }

    public static CustomKnappingRecipeData get(MinecraftServer server) {
        if (server == null) {
            throw new IllegalStateException("Minecraft server is not available");
        }
        CustomKnappingRecipeData data = server.getDataStorage().computeIfAbsent(TYPE);
        data.syncRuntimeState();
        return data;
    }

    public Collection<StoredKnappingRecipe> getRecipes() {
        return Collections.unmodifiableCollection(recipes.values());
    }

    public Set<Identifier> getDisabledRecipes() {
        return Collections.unmodifiableSet(disabled);
    }

    public StoredKnappingRecipe getRecipe(Identifier id) {
        return recipes.get(id);
    }

    public boolean hasOverride(Identifier id) {
        return recipes.containsKey(id);
    }

    /** Adds a new server recipe or overrides a resource recipe with the same id. */
    public void upsertRecipe(StoredKnappingRecipe recipe) {
        recipes.put(recipe.id(), recipe);
        disabled.remove(recipe.id());
        changed();
    }

    /** Removes only the server override/custom recipe; the original resource recipe can reappear. */
    public boolean removeOverride(Identifier id) {
        StoredKnappingRecipe removed = recipes.remove(id);
        if (removed != null) {
            changed();
            return true;
        }
        return false;
    }

    /** Enables or disables the effective recipe id without touching datapack JSON. */
    public void setRecipeDisabled(Identifier id, boolean isDisabled) {
        boolean stateChanged = isDisabled ? disabled.add(id) : disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    /** Restores the datapack/built-in state for an id. */
    public void restoreOriginal(Identifier id) {
        boolean stateChanged = recipes.remove(id) != null;
        stateChanged |= disabled.remove(id);
        if (stateChanged) {
            changed();
        }
    }

    /**
     * Pushes persisted admin recipes into the hot runtime recipe map.
     * No datapack reload or server restart is needed after a GUI save.
     */
    public void syncRuntimeState() {
        Map<Identifier, KnappingRecipe> runtimeRecipes = new LinkedHashMap<>();

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

    private List<StoredKnappingRecipe> recipesForCodec() {
        return new ArrayList<>(recipes.values());
    }

    private List<Identifier> disabledForCodec() {
        return new ArrayList<>(disabled);
    }
}
