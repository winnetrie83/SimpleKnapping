package be.winnetrie.mod.simpleknapping.admin;

import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import com.google.gson.Gson;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Server-authoritative snapshot consumed by both admin editor screens. */
@SuppressWarnings("null")
public record RecipeEditorSnapshot(
        List<String> knappingTypes,
        List<TypeEntry> typeEntries,
        List<RecipeEntry> recipes,
        String notice,
        boolean noticeError
) {
    private static final Gson GSON = new Gson();

    public RecipeEditorSnapshot {
        knappingTypes = List.copyOf(knappingTypes);
        typeEntries = List.copyOf(typeEntries);
        recipes = List.copyOf(recipes);
        notice = notice == null ? "" : notice;
    }

    public static RecipeEditorSnapshot fromServer(String notice, boolean noticeError) {
        List<String> activeTypeIds = KnappingTypeManager.getEffectiveTypes().keySet().stream()
                .map(ResourceLocation::toString)
                .sorted()
                .toList();

        Map<ResourceLocation, KnappingType> resourceTypes = KnappingTypeManager.getResourceTypes();
        Map<ResourceLocation, KnappingType> serverTypes = KnappingTypeManager.getServerTypes();
        Set<ResourceLocation> disabledTypes = KnappingTypeManager.getDisabledTypes();

        Set<ResourceLocation> allTypeIds = new LinkedHashSet<>();
        resourceTypes.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(allTypeIds::add);
        serverTypes.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(allTypeIds::add);
        disabledTypes.stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(allTypeIds::add);

        List<TypeEntry> typeEntries = new ArrayList<>();
        for (ResourceLocation id : allTypeIds) {
            KnappingType resourceType = resourceTypes.get(id);
            KnappingType serverType = serverTypes.get(id);
            KnappingType shownType = serverType != null ? serverType : resourceType;
            if (shownType == null) {
                continue;
            }

            String origin;
            if (resourceType != null && serverType != null) {
                origin = "OVERRIDE";
            } else if (serverType != null) {
                origin = "CUSTOM";
            } else {
                origin = "RESOURCE";
            }

            typeEntries.add(new TypeEntry(
                    id.toString(),
                    BuiltInRegistries.ITEM.getKey(shownType.tool()).toString(),
                    BuiltInRegistries.ITEM.getKey(shownType.material()).toString(),
                    shownType.materialCost(),
                    shownType.textureBlock().toString(),
                    shownType.texture().toString(),
                    origin,
                    disabledTypes.contains(id)
            ));
        }
        typeEntries.sort(Comparator.comparing(TypeEntry::id));

        Map<ResourceLocation, KnappingRecipe> resources = KnappingRecipeManager.getResourceRecipes();
        Map<ResourceLocation, KnappingRecipe> server = KnappingRecipeManager.getServerRecipes();
        Set<ResourceLocation> disabled = KnappingRecipeManager.getDisabledRecipes();

        Set<ResourceLocation> allIds = new LinkedHashSet<>();
        resources.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(allIds::add);
        server.keySet().stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(allIds::add);
        disabled.stream().sorted(Comparator.comparing(ResourceLocation::toString)).forEach(allIds::add);

        List<RecipeEntry> entries = new ArrayList<>();
        for (ResourceLocation id : allIds) {
            KnappingRecipe resourceRecipe = resources.get(id);
            KnappingRecipe serverRecipe = server.get(id);
            KnappingRecipe shownRecipe = serverRecipe != null ? serverRecipe : resourceRecipe;
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

            Item material = KnappingRecipeManager.resolveMaterial(shownRecipe);
            int materialCost = KnappingRecipeManager.resolveMaterialCost(shownRecipe);
            ResourceLocation materialId = material == null || material == Items.AIR
                    ? ResourceLocation.withDefaultNamespace("air")
                    : BuiltInRegistries.ITEM.getKey(material);
            ResourceLocation resultId = BuiltInRegistries.ITEM.getKey(shownRecipe.resultItem());

            entries.add(new RecipeEntry(
                    id.toString(),
                    shownRecipe.knappingType().toString(),
                    materialId.toString(),
                    materialCost,
                    Arrays.asList(shownRecipe.pattern().clone()),
                    resultId.toString(),
                    shownRecipe.resultCount(),
                    origin,
                    disabled.contains(id)
            ));
        }

        entries.sort(Comparator.comparing(RecipeEntry::id));
        return new RecipeEditorSnapshot(activeTypeIds, typeEntries, entries, notice, noticeError);
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
                snapshot.typeEntries == null ? List.of() : snapshot.typeEntries,
                snapshot.recipes == null ? List.of() : snapshot.recipes,
                snapshot.notice,
                snapshot.noticeError
        );
    }

    public record TypeEntry(
            String id,
            String tool,
            String material,
            int materialCost,
            String textureBlock,
            String resolvedTexture,
            String origin,
            boolean disabled
    ) {
        public TypeEntry {
            origin = origin == null ? "RESOURCE" : origin;
            textureBlock = textureBlock == null ? "minecraft:clay" : textureBlock;
            resolvedTexture = resolvedTexture == null ? "minecraft:textures/block/clay.png" : resolvedTexture;
        }

        public boolean hasResourceLayer() {
            return "RESOURCE".equals(origin) || "OVERRIDE".equals(origin);
        }

        public boolean hasServerLayer() {
            return "CUSTOM".equals(origin) || "OVERRIDE".equals(origin);
        }
    }

    public record RecipeEntry(
            String id,
            String knappingType,
            String material,
            int materialCost,
            List<String> pattern,
            String resultItem,
            int resultCount,
            String origin,
            boolean disabled
    ) {
        public RecipeEntry {
            material = material == null ? "minecraft:air" : material;
            materialCost = materialCost < 1 ? 1 : materialCost;
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
