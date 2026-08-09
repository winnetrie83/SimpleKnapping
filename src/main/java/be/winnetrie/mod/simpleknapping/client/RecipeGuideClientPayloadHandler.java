package be.winnetrie.mod.simpleknapping.client;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.client.screen.KnappingRecipeGuideScreen;
import be.winnetrie.mod.simpleknapping.guide.RecipeGuideSnapshot;
import be.winnetrie.mod.simpleknapping.network.RecipeGuidePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Opens the public read-only knapping recipe guide. */
public final class RecipeGuideClientPayloadHandler {
    private RecipeGuideClientPayloadHandler() {
    }

    public static void handle(RecipeGuidePayload payload, IPayloadContext context) {
        if (!"snapshot".equals(payload.kind())) {
            return;
        }

        try {
            RecipeGuideSnapshot snapshot = RecipeGuideSnapshot.fromJson(payload.json());
            Minecraft.getInstance().setScreenAndShow(new KnappingRecipeGuideScreen(snapshot));
        } catch (RuntimeException exception) {
            SimpleKnapping.LOGGER.error("Could not open the knapping recipe guide", exception);
        }
    }
}
