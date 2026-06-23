package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.Config;
import be.winnetrie.mod.simpleknapping.registry.ModItemTags;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class StickDropEvents {

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {

        if (!Config.ENABLE_STICK_DROPS.get()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!event.getState().is(BlockTags.LEAVES)) {
            return;
        }

        ItemStack heldItem = event.getPlayer().getMainHandItem();

        double dropChance = 0.25; // 25%

        if (heldItem.is(ModItemTags.KNIVES)) {
            dropChance = 0.50; // 50%
            heldItem.hurtAndBreak(
                    1,
                    event.getPlayer(),
                    event.getPlayer().getEquipmentSlotForItem(heldItem)
            );
        }

        if (Math.random() > dropChance) {
            return;
        }

        BlockPos pos = event.getPos();

        Block.popResource(level, pos, new ItemStack(Items.STICK, 1));
    }
}