package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.registry.ModItems;
import be.winnetrie.mod.simpleknapping.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

public class PlantFiberEvents {

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {

        if (!Config.ENABLE_PLANT_FIBER_DROPS.get()) {
            return;
        }

        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        Block block = event.getState().getBlock();

        if (block != Blocks.SHORT_GRASS && block != Blocks.TALL_GRASS) {
            return;
        }

        ItemStack heldItem = event.getPlayer().getMainHandItem();

        double dropChance = 0.10; // 10%

        if (heldItem.is(ModItems.FLINT_KNIFE.get())) {
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