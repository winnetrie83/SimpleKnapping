package be.winnetrie.mod.simpleknapping.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public class AdvancementHelper {

    public static boolean hasAdvancement(ServerPlayer player, Identifier id) {

        AdvancementHolder advancement =
                player.level().getServer().getAdvancements().get(id);

        if (advancement == null) {
            return false;
        }

        return player.getAdvancements()
                .getOrStartProgress(advancement)
                .isDone();
    }
}