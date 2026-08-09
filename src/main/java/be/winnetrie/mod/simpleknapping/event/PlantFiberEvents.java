package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.registry.ModItemTags;
import be.winnetrie.mod.simpleknapping.registry.ModItems;
import be.winnetrie.mod.simpleknapping.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

@SuppressWarnings("null")
public class PlantFiberEvents {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {

        if (!Config.ENABLE_PLANT_FIBER_DROPS.get()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Block block = event.getState().getBlock();

        if (block != Blocks.SHORT_GRASS && block != Blocks.TALL_GRASS && block != Blocks.FERN && block != Blocks.LARGE_FERN) {
            return;
        }

        ItemStack heldItem = event.getPlayer().getMainHandItem();

        double dropChance = 0.10; // 10%

        if (heldItem.is(ModItemTags.KNIVES)) {
            dropChance = 0.60; // 60%
            heldItem.hurtAndBreak( 1, event.getPlayer(), event.getPlayer().getEquipmentSlotForItem(heldItem));
        }

        if (Math.random() > dropChance) {
            return;
        }

        BlockPos pos = event.getPos();

        Block.popResource(level, pos, new ItemStack(ModItems.PLANT_FIBER.get(), 1));

        
    }
}