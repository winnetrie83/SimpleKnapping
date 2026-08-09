package be.winnetrie.mod.simpleknapping.item;

import be.winnetrie.mod.simpleknapping.knapping.KnappingInteraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The bundled flint knapping tool remains supported, but the actual matching
 * now lives in KnappingInteraction so configured vanilla/modded tools work too.
 */
public class KnappingToolItem extends Item {

    public KnappingToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return KnappingInteraction.tryOpen(serverPlayer, hand)
                    ? InteractionResult.SUCCESS
                    : InteractionResult.PASS;
        }

        return KnappingInteraction.findType(player, hand) != null
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }
}
