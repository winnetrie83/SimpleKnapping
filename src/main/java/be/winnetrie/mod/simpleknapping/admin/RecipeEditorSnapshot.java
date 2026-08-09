package be.winnetrie.mod.simpleknapping.admin;

import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import com.google.gson.Gson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Server-authoritative snapshot consumed by the client recipe editor.
 * Only registry ids and primitive values cross the network.
 */
public record RecipeEditorSnapshot(
        List<String> knappingTypes,
        List<RecipeEntry> recipes,
        String notice,
        boolean noticeError
) {
    private static final Gson GSON = new Gson();

    public RecipeEditorSnapshot {
        knappingTypes = List.copyOf(knappingTypes);
        recipes = List.copyOf(recipes);
        notice = notice == null ? "" : notice;
    }

    public static RecipeEditorSnapshot fromServer(String notice, boolean noticeError) {
        List<String> typeIds = KnappingTypeManager.KNAPPING_TYPES.keySet().stream()
                .map(Identifier::toString)
                .sorted()
                .toList();

        Map<Identifier, KnappingRecipe> resources = KnappingRecipeManager.getResourceRecipes();
        Map<Identifier, KnappingRecipe> server = KnappingRecipeManager.getServerRecipes();
        Set<Identifier> disabled = KnappingRecipeManager.getDisabledRecipes();

        Set<Identifier> allIds = new LinkedHashSet<>();
        resources.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).forEach(allIds::add);
        server.keySet().stream().sorted(Comparator.comparing(Identifier::toString)).forEach(allIds::add);
        disabled.stream().sorted(Comparator.comparing(Identifier::toString)).forEach(allIds::add);

        List<RecipeEntry> entries = new ArrayList<>();
        for (Identifier id : allIds) {
            KnappingRecipe resourceRecipe = resources.get(id);
            KnappingRecipe serverRecipe = server.get(id);
            KnappingRecipe shownRecipe = serverRecipe != null ? serverRecipe : resourceRecipe;

            // Ignore stale disabled ids if neither source still defines the recipe.
            if (shownRecipe == null) {
                continue;
            }

            String origin;
            if (resourceRecipe != null && serverRecipe != null) {
                origin = "OVERRIDE";
            } else if (serverRecipe != null) {
                origin = "CUSTOM";
            } else {
                origin = "RESOURCE";
            }

            Identifier resultId = BuiltInRegistries.ITEM.getKey(shownRecipe.resultItem());
            entries.add(new RecipeEntry(
                    id.toString(),
                    shownRecipe.knappingType().toString(),
                    Arrays.asList(shownRecipe.pattern().clone()),
                    resultId.toString(),
                    shownRecipe.resultCount(),
                    origin,
                    disabled.contains(id)
            ));
        }

        entries.sort(Comparator.comparing(RecipeEntry::id));
        return new RecipeEditorSnapshot(typeIds, entries, notice, noticeError);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static RecipeEditorSnapshot fromJson(String json) {
        RecipeEditorSnapshot snapshot = GSON.fromJson(json, RecipeEditorSnapshot.class);
        if (snapshot == null) {
            throw new IllegalArgumentException("Missing recipe editor snapshot");
        }
        return new RecipeEditorSnapshot(
                snapshot.knappingTypes == null ? List.of() : snapshot.knappingTypes,
                snapshot.recipes == null ? List.of() : snapshot.recipes,
                snapshot.notice,
                snapshot.noticeError
        );
    }

    public record RecipeEntry(
            String id,
            String knappingType,
            List<String> pattern,
            String resultItem,
            int resultCount,
            String origin,
            boolean disabled
    ) {
        public RecipeEntry {
            pattern = pattern == null ? List.of() : List.copyOf(pattern);
            origin = origin == null ? "RESOURCE" : origin;
        }

        public boolean hasResourceLayer() {
            return "RESOURCE".equals(origin) || "OVERRIDE".equals(origin);
        }

        public boolean hasServerLayer() {
            return "CUSTOM".equals(origin) || "OVERRIDE".equals(origin);
        }
    }
}
