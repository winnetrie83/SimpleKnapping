package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.admin.RecipeEditorSnapshot;
import be.winnetrie.mod.simpleknapping.command.SimpleKnappingCommands;
import be.winnetrie.mod.simpleknapping.client.RecipeEditorClientPayloadHandler;
import be.winnetrie.mod.simpleknapping.knapping.CustomKnappingRecipeData;
import be.winnetrie.mod.simpleknapping.knapping.CustomKnappingTypeData;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.knapping.StoredKnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.StoredKnappingType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@SuppressWarnings("null")
public final class RecipeEditorNetwork {
    private static final String NETWORK_VERSION = "recipe_editor_5";

    private RecipeEditorNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playBidirectional(
                RecipeEditorPayload.TYPE,
                RecipeEditorPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        RecipeEditorClientPayloadHandler::handle,
                        RecipeEditorNetwork::handleServerPayload
                )
        );
        SettingsNetwork.register(registrar);
        RecipeGuideNetwork.register(registrar);
    }

    public static void openEditor(ServerPlayer player) {
        if (!SimpleKnappingCommands.canEdit(player)) {
            return;
        }
        sendSnapshot(player, "", false);
    }

    private static void handleServerPayload(RecipeEditorPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!"action".equals(payload.kind())) {
            return;
        }
        if (!SimpleKnappingCommands.canEdit(player)) {
            sendSnapshot(player, "You no longer have permission to edit knapping recipes/types.", true);
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
            String action = requireString(root, "action");
            switch (action) {
                case "save" -> handleSaveRecipe(player, root);
                case "set_disabled" -> handleSetRecipeDisabled(player, root);
                case "remove_override" -> handleRemoveRecipeOverride(player, root);
                case "restore_original" -> handleRestoreOriginalRecipe(player, root);
                case "save_type" -> handleSaveType(player, root);
                case "set_type_disabled" -> handleSetTypeDisabled(player, root);
                case "remove_type_override" -> handleRemoveTypeOverride(player, root);
                case "restore_type_original" -> handleRestoreOriginalType(player, root);
                default -> sendSnapshot(player, "Unknown recipe/type editor action: " + action, true);
            }
        } catch (RuntimeException exception) {
            SimpleKnapping.LOGGER.warn("Rejected invalid recipe/type editor request from {}", player.getScoreboardName(), exception);
            sendSnapshot(player, "Change rejected: " + safeMessage(exception), true);
        }
    }

    // ---------------------------------------------------------------------
    // Recipes
    // ---------------------------------------------------------------------

    private static void handleSaveRecipe(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "recipe id");
        ResourceLocation typeId = parseResourceLocation(requireString(root, "knapping_type"), "knapping type");
        ResourceLocation materialId = parseResourceLocation(requireString(root, "material"), "recipe material");
        ResourceLocation resultId = parseResourceLocation(requireString(root, "result_item"), "result item");
        int materialCost = root.get("material_cost").getAsInt();
        int count = root.get("result_count").getAsInt();

        KnappingType type = KnappingTypeManager.get(typeId);
        if (type == null) {
            throw new IllegalArgumentException("Unknown or disabled knapping type: " + typeId);
        }

        Item material = requireUsableItem(materialId, "recipe material");
        requireUsableItem(resultId, "recipe result");
        if (materialCost < 1 || materialCost > 99) {
            throw new IllegalArgumentException("Material amount must be between 1 and 99");
        }
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("Result count must be between 1 and 99");
        }

        List<String> pattern = readPattern(root.getAsJsonArray("pattern"));
        StoredKnappingRecipe.validatePattern(pattern);

        ResourceLocation conflict = findActivePatternConflict(id, typeId, material, pattern);
        if (conflict != null) {
            throw new IllegalArgumentException("That pattern is already used with this material by " + conflict);
        }

        ResourceLocation routeConflict = findActiveRecipeMaterialTypeConflict(id, typeId, material);
        if (routeConflict != null) {
            throw new IllegalArgumentException(
                    "This tool + material is already routed to another knapping type by recipe " + routeConflict
            );
        }

        StoredKnappingRecipe stored = new StoredKnappingRecipe(
                id, typeId, pattern, materialId, materialCost, resultId, count
        );
        CustomKnappingRecipeData.get(player.level().getServer()).upsertRecipe(stored);
        sendSnapshot(player, "Saved recipe " + id + ". It is active immediately.", false);
    }

    private static void handleSetRecipeDisabled(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "recipe id");
        boolean disabled = root.get("disabled").getAsBoolean();

        if (!recipeExistsInAnyLayer(id)) {
            throw new IllegalArgumentException("Recipe no longer exists: " + id);
        }

        if (!disabled) {
            KnappingRecipe candidate = recipeForId(id);
            if (candidate != null) {
                if (KnappingTypeManager.get(candidate.knappingType()) == null) {
                    throw new IllegalArgumentException("Cannot enable: its knapping type is missing or disabled");
                }
                Item material = KnappingRecipeManager.resolveMaterial(candidate);
                ResourceLocation conflict = findActivePatternConflict(
                        id,
                        candidate.knappingType(),
                        material,
                        Arrays.asList(candidate.pattern().clone())
                );
                if (conflict != null) {
                    throw new IllegalArgumentException("Cannot enable: pattern + material is already used by " + conflict);
                }
                ResourceLocation routeConflict = findActiveRecipeMaterialTypeConflict(id, candidate.knappingType(), material);
                if (routeConflict != null) {
                    throw new IllegalArgumentException(
                            "Cannot enable: its tool + material is already routed by recipe " + routeConflict
                    );
                }
            }
        }

        CustomKnappingRecipeData.get(player.level().getServer()).setRecipeDisabled(id, disabled);
        sendSnapshot(player, (disabled ? "Disabled " : "Enabled ") + id + ".", false);
    }

    private static void handleRemoveRecipeOverride(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "recipe id");
        CustomKnappingRecipeData data = CustomKnappingRecipeData.get(player.level().getServer());
        boolean hasResourceOriginal = KnappingRecipeManager.getResourceRecipes().containsKey(id);
        if (!data.hasOverride(id)) {
            throw new IllegalArgumentException("There is no server recipe/override for " + id);
        }
        if (hasResourceOriginal) {
            data.removeOverride(id);
        } else {
            data.restoreOriginal(id);
        }
        sendSnapshot(player, "Removed the server recipe layer for " + id + ".", false);
    }

    private static void handleRestoreOriginalRecipe(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "recipe id");
        CustomKnappingRecipeData.get(player.level().getServer()).restoreOriginal(id);
        sendSnapshot(player, "Restored the resource/datapack recipe state for " + id + ".", false);
    }

    // ---------------------------------------------------------------------
    // Knapping types
    // ---------------------------------------------------------------------

    private static void handleSaveType(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "knapping type id");
        ResourceLocation toolId = parseResourceLocation(requireString(root, "tool"), "knapping tool");
        ResourceLocation materialId = parseResourceLocation(requireString(root, "material"), "knapping material");
        ResourceLocation textureBlockId = parseResourceLocation(requireString(root, "texture_block"), "fallback texture block");
        int materialCost = root.get("material_cost").getAsInt();

        requireUsableItem(toolId, "knapping tool");
        requireUsableItem(materialId, "knapping material");
        requireUsableBlock(textureBlockId, "fallback texture block");
        if (materialCost < 1 || materialCost > 99) {
            throw new IllegalArgumentException("Material amount must be between 1 and 99");
        }

        StoredKnappingType stored = new StoredKnappingType(
                id,
                toolId,
                materialId,
                materialCost,
                textureBlockId
        );
        KnappingType candidate = stored.toRuntimeType();
        ResourceLocation conflict = findActiveTypeRecipeConflict(id, candidate);
        if (conflict != null) {
            throw new IllegalArgumentException(
                    "This tool would make a recipe material ambiguous with active recipe " + conflict
            );
        }
        CustomKnappingTypeData.get(player.level().getServer()).upsertType(stored);
        sendSnapshot(player, "Saved knapping type " + id + ". It is active immediately.", false);
    }

    private static void handleSetTypeDisabled(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "knapping type id");
        boolean disabled = root.get("disabled").getAsBoolean();

        if (!typeExistsInAnyLayer(id)) {
            throw new IllegalArgumentException("Knapping type no longer exists: " + id);
        }

        if (!disabled) {
            KnappingType candidate = typeForId(id);
            if (candidate == null) {
                throw new IllegalArgumentException("Cannot enable invalid knapping type " + id);
            }
            ResourceLocation conflict = findActiveTypeRecipeConflict(id, candidate);
            if (conflict != null) {
                throw new IllegalArgumentException(
                        "Cannot enable: this tool would make recipe material routing ambiguous with " + conflict
                );
            }
        }

        CustomKnappingTypeData.get(player.level().getServer()).setTypeDisabled(id, disabled);
        sendSnapshot(player, (disabled ? "Disabled knapping type " : "Enabled knapping type ") + id + ".", false);
    }

    private static void handleRemoveTypeOverride(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "knapping type id");
        CustomKnappingTypeData data = CustomKnappingTypeData.get(player.level().getServer());
        boolean hasResourceOriginal = KnappingTypeManager.getResourceTypes().containsKey(id);
        if (!data.hasOverride(id)) {
            throw new IllegalArgumentException("There is no server knapping type/override for " + id);
        }
        if (hasResourceOriginal) {
            data.removeOverride(id);
        } else {
            data.restoreOriginal(id);
        }
        sendSnapshot(player, "Removed the server knapping type layer for " + id + ".", false);
    }

    private static void handleRestoreOriginalType(ServerPlayer player, JsonObject root) {
        ResourceLocation id = parseResourceLocation(requireString(root, "id"), "knapping type id");
        CustomKnappingTypeData.get(player.level().getServer()).restoreOriginal(id);
        sendSnapshot(player, "Restored the resource/datapack knapping type state for " + id + ".", false);
    }

    // ---------------------------------------------------------------------
    // Validation/helpers
    // ---------------------------------------------------------------------

    private static Item requireUsableItem(ResourceLocation id, String label) {
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            throw new IllegalArgumentException("Unknown " + label + ": " + id);
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR) {
            throw new IllegalArgumentException("Air cannot be used as " + label);
        }
        return item;
    }

    private static Block requireUsableBlock(ResourceLocation id, String label) {
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            throw new IllegalArgumentException("Unknown " + label + ": " + id);
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        if (block == Blocks.AIR) {
            throw new IllegalArgumentException("Air cannot be used as " + label);
        }
        return block;
    }

    private static boolean recipeExistsInAnyLayer(ResourceLocation id) {
        return KnappingRecipeManager.getResourceRecipes().containsKey(id)
                || KnappingRecipeManager.getServerRecipes().containsKey(id);
    }

    private static KnappingRecipe recipeForId(ResourceLocation id) {
        KnappingRecipe server = KnappingRecipeManager.getServerRecipes().get(id);
        return server != null ? server : KnappingRecipeManager.getResourceRecipes().get(id);
    }

    private static boolean typeExistsInAnyLayer(ResourceLocation id) {
        return KnappingTypeManager.getResourceTypes().containsKey(id)
                || KnappingTypeManager.getServerTypes().containsKey(id);
    }

    private static KnappingType typeForId(ResourceLocation id) {
        KnappingType server = KnappingTypeManager.getServerTypes().get(id);
        return server != null ? server : KnappingTypeManager.getResourceTypes().get(id);
    }

    private static ResourceLocation findActivePatternConflict(ResourceLocation editedId,
                                                        ResourceLocation typeId,
                                                        Item material,
                                                        List<String> pattern) {
        String[] target = pattern.toArray(String[]::new);
        for (Map.Entry<ResourceLocation, KnappingRecipe> entry : KnappingRecipeManager.getEffectiveRecipes().entrySet()) {
            if (entry.getKey().equals(editedId)) {
                continue;
            }
            KnappingRecipe recipe = entry.getValue();
            if (recipe.knappingType().equals(typeId)
                    && KnappingRecipeManager.resolveMaterial(recipe) == material
                    && Arrays.equals(recipe.pattern(), target)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * One held tool + input material must resolve to exactly one active type.
     * Different recipes inside that same type may freely share the material.
     */
    private static ResourceLocation findActiveRecipeMaterialTypeConflict(ResourceLocation editedRecipeId,
                                                                   ResourceLocation editedTypeId,
                                                                   Item material) {
        KnappingType editedType = KnappingTypeManager.get(editedTypeId);
        if (editedType == null) {
            return null;
        }

        for (Map.Entry<ResourceLocation, KnappingRecipe> entry : KnappingRecipeManager.getEffectiveRecipes().entrySet()) {
            if (entry.getKey().equals(editedRecipeId)) {
                continue;
            }
            KnappingRecipe otherRecipe = entry.getValue();
            if (otherRecipe.knappingType().equals(editedTypeId)) {
                continue;
            }
            KnappingType otherType = KnappingTypeManager.get(otherRecipe.knappingType());
            if (otherType != null
                    && otherType.tool() == editedType.tool()
                    && KnappingRecipeManager.resolveMaterial(otherRecipe) == material) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Checks the recipe-material routes that would exist after changing/enabling
     * a type. Legacy recipes without an explicit material use candidate.material().
     */
    private static ResourceLocation findActiveTypeRecipeConflict(ResourceLocation editedTypeId, KnappingType candidate) {
        for (Map.Entry<ResourceLocation, KnappingRecipe> ownEntry : KnappingRecipeManager.getEffectiveRecipes().entrySet()) {
            KnappingRecipe ownRecipe = ownEntry.getValue();
            if (!ownRecipe.knappingType().equals(editedTypeId)) {
                continue;
            }

            Item ownMaterial = ownRecipe.material() != null
                    ? ownRecipe.material()
                    : candidate.material();

            for (Map.Entry<ResourceLocation, KnappingRecipe> otherEntry : KnappingRecipeManager.getEffectiveRecipes().entrySet()) {
                KnappingRecipe otherRecipe = otherEntry.getValue();
                if (otherRecipe.knappingType().equals(editedTypeId)) {
                    continue;
                }
                KnappingType otherType = KnappingTypeManager.get(otherRecipe.knappingType());
                if (otherType != null
                        && otherType.tool() == candidate.tool()
                        && KnappingRecipeManager.resolveMaterial(otherRecipe) == ownMaterial) {
                    return otherEntry.getKey();
                }
            }
        }
        return null;
    }

    private static List<String> readPattern(JsonArray array) {
        if (array == null) {
            throw new IllegalArgumentException("Missing 5x5 pattern");
        }
        List<String> rows = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            rows.add(array.get(i).getAsString());
        }
        return rows;
    }

    private static String requireString(JsonObject root, String key) {
        if (!root.has(key)) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return root.get(key).getAsString();
    }

    private static ResourceLocation parseResourceLocation(String value, String label) {
        try {
            return ResourceLocation.parse(value);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Invalid " + label + ": " + value);
        }
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "invalid data" : message;
    }

    public static void sendSnapshot(ServerPlayer player, String notice, boolean error) {
        RecipeEditorSnapshot snapshot = RecipeEditorSnapshot.fromServer(notice, error);
        PacketDistributor.sendToPlayer(player, RecipeEditorPayload.snapshot(snapshot.toJson()));
    }
}
