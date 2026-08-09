package be.winnetrie.mod.simpleknapping.client;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.admin.RecipeEditorSnapshot;
import be.winnetrie.mod.simpleknapping.client.screen.KnappingRecipeEditorScreen;
import be.winnetrie.mod.simpleknapping.network.RecipeEditorPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class RecipeEditorClientPayloadHandler {
    private RecipeEditorClientPayloadHandler() {
    }

    public static void handle(RecipeEditorPayload payload, IPayloadContext context) {
        if (!"snapshot".equals(payload.kind())) {
            return;
        }

        try {
            RecipeEditorSnapshot snapshot = RecipeEditorSnapshot.fromJson(payload.json());
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.gui.screen() instanceof KnappingRecipeEditorScreen screen) {
                screen.applyServerSnapshot(snapshot);
            } else {
                minecraft.setScreenAndShow(new KnappingRecipeEditorScreen(snapshot));
            }
        } catch (RuntimeException exception) {
            SimpleKnapping.LOGGER.error("Could not open/update the knapping recipe editor", exception);
        }
    }
}
