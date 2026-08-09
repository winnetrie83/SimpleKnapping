package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.admin.RecipeEditorSnapshot;
import be.winnetrie.mod.simpleknapping.command.SimpleKnappingCommands;
import be.winnetrie.mod.simpleknapping.knapping.CustomKnappingRecipeData;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipe;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.knapping.StoredKnappingRecipe;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class RecipeEditorNetwork {
    private static final String NETWORK_VERSION = "recipe_editor_1";

    private RecipeEditorNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(NETWORK_VERSION);
        registrar.playBidirectional(
                RecipeEditorPayload.TYPE,
                RecipeEditorPayload.STREAM_CODEC,
                RecipeEditorNetwork::handleServerPayload
        );
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
            sendSnapshot(player, "You no longer have permission to edit knapping recipes.", true);
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
            String action = requireString(root, "action");
            switch (action) {
                case "save" -> handleSave(player, root);
                case "set_disabled" -> handleSetDisabled(player, root);
                case "remove_override" -> handleRemoveOverride(player, root);
                case "restore_original" -> handleRestoreOriginal(player, root);
                default -> sendSnapshot(player, "Unknown recipe editor action: " + action, true);
            }
        } catch (RuntimeException exception) {
            SimpleKnapping.LOGGER.warn("Rejected invalid recipe editor request from {}", player.getScoreboardName(), exception);
            sendSnapshot(player, "Recipe change rejected: " + safeMessage(exception), true);
        }
    }

    private static void handleSave(ServerPlayer player, JsonObject root) {
        Identifier id = parseIdentifier(requireString(root, "id"), "recipe id");
        Identifier typeId = parseIdentifier(requireString(root, "knapping_type"), "knapping type");
        Identifier resultId = parseIdentifier(requireString(root, "result_item"), "result item");
        int count = root.get("result_count").getAsInt();

        if (KnappingTypeManager.get(typeId) == null) {
            throw new IllegalArgumentException("Unknown knapping type: " + typeId);
        }
        if (!BuiltInRegistries.ITEM.containsKey(resultId)) {
            throw new IllegalArgumentException("Unknown result item: " + resultId);
        }

        Item resultItem = BuiltInRegistries.ITEM.getValue(resultId);
        if (resultItem == Items.AIR) {
            throw new IllegalArgumentException("Air cannot be used as a recipe result");
        }
        if (count < 1 || count > 99) {
            throw new IllegalArgumentException("Result count must be between 1 and 99");
        }

        List<String> pattern = readPattern(root.getAsJsonArray("pattern"));
        StoredKnappingRecipe.validatePattern(pattern);

        Identifier conflict = findActivePatternConflict(id, typeId, pattern);
        if (conflict != null) {
            throw new IllegalArgumentException("That pattern is already used by " + conflict);
        }

        StoredKnappingRecipe stored = new StoredKnappingRecipe(id, typeId, pattern, resultId, count);
        CustomKnappingRecipeData.get(player.level().getServer()).upsertRecipe(stored);
        sendSnapshot(player, "Saved recipe " + id + ". It is active immediately.", false);
    }

    private static void handleSetDisabled(ServerPlayer player, JsonObject root) {
        Identifier id = parseIdentifier(requireString(root, "id"), "recipe id");
        boolean disabled = root.get("disabled").getAsBoolean();

        if (!recipeExistsInAnyLayer(id)) {
            throw new IllegalArgumentException("Recipe no longer exists: " + id);
        }

        if (!disabled) {
            KnappingRecipe candidate = recipeForId(id);
            if (candidate != null) {
                Identifier conflict = findActivePatternConflict(
                        id,
                        candidate.knappingType(),
                        Arrays.asList(candidate.pattern().clone())
                );
                if (conflict != null) {
                    throw new IllegalArgumentException("Cannot enable: pattern is already used by " + conflict);
                }
            }
        }

        CustomKnappingRecipeData.get(player.level().getServer()).setRecipeDisabled(id, disabled);
        sendSnapshot(player, (disabled ? "Disabled " : "Enabled ") + id + ".", false);
    }

    private static void handleRemoveOverride(ServerPlayer player, JsonObject root) {
        Identifier id = parseIdentifier(requireString(root, "id"), "recipe id");
        CustomKnappingRecipeData data = CustomKnappingRecipeData.get(player.level().getServer());
        boolean hasResourceOriginal = KnappingRecipeManager.getResourceRecipes().containsKey(id);
        if (!data.hasOverride(id)) {
            throw new IllegalArgumentException("There is no server recipe/override for " + id);
        }
        if (hasResourceOriginal) {
            data.removeOverride(id);
        } else {
            // Custom-only recipe: also remove a possible disabled marker so no stale id remains on disk.
            data.restoreOriginal(id);
        }
        sendSnapshot(player, "Removed the server layer for " + id + ".", false);
    }

    private static void handleRestoreOriginal(ServerPlayer player, JsonObject root) {
        Identifier id = parseIdentifier(requireString(root, "id"), "recipe id");
        CustomKnappingRecipeData.get(player.level().getServer()).restoreOriginal(id);
        sendSnapshot(player, "Restored the resource/datapack state for " + id + ".", false);
    }

    private static boolean recipeExistsInAnyLayer(Identifier id) {
        return KnappingRecipeManager.getResourceRecipes().containsKey(id)
                || KnappingRecipeManager.getServerRecipes().containsKey(id);
    }

    private static KnappingRecipe recipeForId(Identifier id) {
        KnappingRecipe server = KnappingRecipeManager.getServerRecipes().get(id);
        return server != null ? server : KnappingRecipeManager.getResourceRecipes().get(id);
    }

    private static Identifier findActivePatternConflict(Identifier editedId,
                                                        Identifier typeId,
                                                        List<String> pattern) {
        String[] target = pattern.toArray(String[]::new);
        for (Map.Entry<Identifier, KnappingRecipe> entry : KnappingRecipeManager.getEffectiveRecipes().entrySet()) {
            if (entry.getKey().equals(editedId)) {
                continue;
            }

            KnappingRecipe recipe = entry.getValue();
            if (!recipe.knappingType().equals(typeId)) {
                continue;
            }

            if (Arrays.equals(recipe.pattern(), target)) {
                return entry.getKey();
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

    private static Identifier parseIdentifier(String value, String label) {
        try {
            return Identifier.parse(value);
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
