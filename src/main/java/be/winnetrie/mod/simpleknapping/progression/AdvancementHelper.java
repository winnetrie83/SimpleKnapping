package be.winnetrie.mod.simpleknapping.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@SuppressWarnings("null")
public class AdvancementHelper {

    public static boolean hasAdvancement(ServerPlayer player, ResourceLocation id) {

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            return false;
        }

        AdvancementHolder advancement = server.getAdvancements().get(id);
        if (advancement == null) {
            return false;
        }

        return player.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }
}