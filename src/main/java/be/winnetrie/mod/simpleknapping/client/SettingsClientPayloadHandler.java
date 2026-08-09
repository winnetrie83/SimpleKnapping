package be.winnetrie.mod.simpleknapping.client;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.client.screen.SimpleKnappingSettingsScreen;
import be.winnetrie.mod.simpleknapping.network.SettingsPayload;
import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client handler for both silent tier sync and the admin settings GUI snapshot. */
public final class SettingsClientPayloadHandler {
    private SettingsClientPayloadHandler() {
    }

    public static void handle(SettingsPayload payload, IPayloadContext context) {
        if (!"snapshot".equals(payload.kind()) && !"state".equals(payload.kind())) {
            return;
        }

        try {
            JsonObject root = JsonParser.parseString(payload.json()).getAsJsonObject();
            boolean wooden = root.get("wooden_tools").getAsBoolean();
            boolean stone = root.get("stone_tools").getAsBoolean();
            boolean treePunching = root.get("tree_punching").getAsBoolean();

            // Needed by the Slot mixin so disabled crafting outputs can disappear
            // immediately on remote clients as well as in singleplayer.
            DisabledVanillaEquipment.setRuntimeTierState(wooden, stone);

            if ("state".equals(payload.kind())) {
                return;
            }

            String notice = root.has("notice") ? root.get("notice").getAsString() : "";
            boolean noticeError = root.has("notice_error") && root.get("notice_error").getAsBoolean();

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof SimpleKnappingSettingsScreen screen) {
                screen.applyServerSnapshot(wooden, stone, treePunching, notice, noticeError);
            } else {
                minecraft.setScreenAndShow(new SimpleKnappingSettingsScreen(
                        wooden,
                        stone,
                        treePunching,
                        notice,
                        noticeError
                ));
            }
        } catch (RuntimeException exception) {
            SimpleKnapping.LOGGER.error("Could not apply Simple Knapping settings/state", exception);
        }
    }
}
