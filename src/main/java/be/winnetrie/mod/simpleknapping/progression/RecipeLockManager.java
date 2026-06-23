package be.winnetrie.mod.simpleknapping.progression;

import be.winnetrie.mod.simpleknapping.Config;
import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class RecipeLockManager {

    private static final Identifier FIRST_FLINT =
            Identifier.parse(SimpleKnapping.MODID + ":progression/first_flint");

    public static boolean isLocked(Player player, ItemStack result) {

        if (!Config.ENABLE_PROGRESSION.get()) {
            return false;
        }

        if (result.is(Items.WOODEN_PICKAXE)) {

            if (player instanceof ServerPlayer serverPlayer) {
                return !AdvancementHelper.hasAdvancement(serverPlayer, FIRST_FLINT);
            }

            return true;
        }

        return false;
    }
}