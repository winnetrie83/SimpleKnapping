package be.winnetrie.mod.simpleknapping.network;

import be.winnetrie.mod.simpleknapping.Config;
import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.command.SimpleKnappingCommands;
import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Server-authoritative networking for /simpleknapping settings. */
@SuppressWarnings("null")
public final class SettingsNetwork {
    private SettingsNetwork() {
    }

    static void register(PayloadRegistrar registrar) {
        registrar.playBidirectional(
                SettingsPayload.TYPE,
                SettingsPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        ClientPayloadBridge::handleSettings,
                        SettingsNetwork::handleServerPayload
                )
        );
    }

    public static void openSettings(ServerPlayer player) {
        if (!SimpleKnappingCommands.canEdit(player)) {
            return;
        }
        sendSnapshot(player, "", false);
    }

    /** Keep every client aware of the tier state used for instant result hiding. */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DisabledVanillaEquipment.syncRuntimeStateFromConfig();
            sendRuntimeState(player);
        }
    }

    private static void handleServerPayload(SettingsPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        if (!"action".equals(payload.kind())) {
            return;
        }
        if (!SimpleKnappingCommands.canEdit(player)) {
            sendSnapshot(player, "You no longer have permission to change Simple Knapping settings.", true);
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
            String action = requireString(root, "action");
            if (!"set_feature".equals(action)) {
                throw new IllegalArgumentException("Unknown settings action: " + action);
            }

            String key = requireString(root, "key");
            if (!root.has("enabled")) {
                throw new IllegalArgumentException("Missing field: enabled");
            }
            boolean enabled = root.get("enabled").getAsBoolean();

            String notice;
            switch (key) {
                case "wooden_tools" -> {
                    Config.setWoodenToolsAndWeaponsEnabled(enabled);
                    notice = "Wooden tools & weapons " + (enabled ? "enabled" : "disabled")
                            + ". Crafting changed immediately; survival sources now use flint replacements.";
                }
                case "stone_tools" -> {
                    Config.setStoneToolsAndWeaponsEnabled(enabled);
                    notice = "Stone tools & weapons " + (enabled ? "enabled" : "disabled")
                            + ". Crafting changed immediately; survival sources now use flint replacements.";
                }
                case "tree_punching" -> {
                    Config.setTreePunchingEnabled(enabled);
                    notice = "Tree punching " + (enabled ? "enabled" : "disabled") + ".";
                }
                default -> throw new IllegalArgumentException("Unknown feature: " + key);
            }

            DisabledVanillaEquipment.syncRuntimeStateFromConfig();
            broadcastRuntimeState(player.level().getServer());
            sendSnapshot(player, notice, false);
        } catch (RuntimeException exception) {
            SimpleKnapping.LOGGER.warn("Rejected invalid settings request from {}", player.getScoreboardName(), exception);
            String message = exception.getMessage();
            sendSnapshot(player, "Change rejected: " + (message == null ? "invalid data" : message), true);
        }
    }

    public static void sendSnapshot(ServerPlayer player, String notice, boolean error) {
        JsonObject root = stateJson();
        root.addProperty("notice", notice == null ? "" : notice);
        root.addProperty("notice_error", error);
        PacketDistributor.sendToPlayer(player, SettingsPayload.snapshot(root.toString()));
    }

    public static void sendRuntimeState(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, SettingsPayload.state(stateJson().toString()));
    }

    public static void broadcastRuntimeState(MinecraftServer server) {
        for (ServerPlayer online : server.getPlayerList().getPlayers()) {
            sendRuntimeState(online);
        }
    }

    private static JsonObject stateJson() {
        JsonObject root = new JsonObject();
        root.addProperty("wooden_tools", Config.woodenToolsAndWeaponsEnabled());
        root.addProperty("stone_tools", Config.stoneToolsAndWeaponsEnabled());
        root.addProperty("tree_punching", Config.treePunchingEnabled());
        return root;
    }

    private static String requireString(JsonObject root, String key) {
        if (!root.has(key)) {
            throw new IllegalArgumentException("Missing field: " + key);
        }
        return root.get(key).getAsString();
    }
}
