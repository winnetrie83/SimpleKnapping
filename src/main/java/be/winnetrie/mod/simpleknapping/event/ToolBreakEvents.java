package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.Config;
import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Enforces the optional no-tree-punching progression rule. */
public class ToolBreakEvents {

    @SubscribeEvent
    public static void onBreakBlock(BreakBlockEvent event) {
        ItemStack heldItem = event.getPlayer().getMainHandItem();

        // Disabled wooden/stone tools are intentionally inert in survival,
        // including copies that entered the inventory through /give before or
        // after the tier was disabled. Creative keeps normal vanilla behavior.
        if (!event.getPlayer().hasInfiniteMaterials() && DisabledVanillaEquipment.isDisabled(heldItem)) {
            event.setCanceled(true);
            return;
        }

        // ON means vanilla-style tree punching is allowed. OFF (default)
        // preserves Simple Knapping's existing requirement for an axe.
        if (Config.treePunchingEnabled()) {
            return;
        }

        if (!event.getState().is(BlockTags.LOGS)) {
            return;
        }

        if (heldItem.is(ItemTags.AXES)) {
            return;
        }

        event.setCanceled(true);
        if (!event.getPlayer().level().isClientSide()) {
            event.getPlayer().sendSystemMessage(Component.literal("You need an axe to chop trees."));
        }
    }
}
