package be.winnetrie.mod.simpleknapping.item;

import be.winnetrie.mod.simpleknapping.knapping.KnappingInteraction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The bundled flint knapping tool remains supported, but the actual matching
 * now lives in KnappingInteraction so configured vanilla/modded tools work too.
 */
@SuppressWarnings("null")
public class KnappingToolItem extends Item {

    public KnappingToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(stack);
        }

        if (player instanceof ServerPlayer serverPlayer) {
            return KnappingInteraction.tryOpen(serverPlayer, hand)
                    ? InteractionResultHolder.success(stack)
                    : InteractionResultHolder.pass(stack);
        }

        return KnappingInteraction.findType(player, hand) != null
                ? InteractionResultHolder.success(stack)
                : InteractionResultHolder.pass(stack);
    }
}
