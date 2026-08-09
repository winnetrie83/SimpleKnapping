package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.knapping.KnappingInteraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Lets any configured vanilla/modded item act as a knapping tool. */
public final class KnappingInteractionEvents {
    private KnappingInteractionEvents() {
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getHand() != InteractionHand.MAIN_HAND || event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        if (KnappingInteraction.tryOpen(serverPlayer, event.getHand())) {
            // RightClickItem cancellation ends the normal Item#use pipeline; our
            // configured knapping action has already handled the interaction.
            event.setCanceled(true);
        }
    }
}
