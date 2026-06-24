package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class ToolBreakEvents {

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {

        

        if (!Config.REQUIRE_AXE_FOR_LOGS.get()) {
            return;
        }

        if (!event.getState().is(BlockTags.LOGS)) {
            return;
        }

        ItemStack heldItem = event.getPlayer().getMainHandItem();

        if (heldItem.is(ItemTags.AXES)) {
            return;
        }

        event.setCanceled(true);

        if (!event.getPlayer().level().isClientSide()) {
            event.getPlayer().sendSystemMessage(Component.literal("You need an axe to chop trees."));
        }
    }
}