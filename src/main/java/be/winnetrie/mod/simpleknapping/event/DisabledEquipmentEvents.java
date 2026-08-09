package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;

/**
 * Runtime progression handling for disabled vanilla wooden/stone equipment.
 *
 * Normal acquisition sources are converted to flint equivalents. Commands and
 * creative inventory can still create the original vanilla stack; those stacks
 * remain deliberately inert for non-creative players while the tier is off.
 */
@SuppressWarnings("null")
public final class DisabledEquipmentEvents {
    private DisabledEquipmentEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        // Natural mob equipment (notably a Wither Skeleton's stone sword) is
        // assigned before the entity joins the server level. Replace it in the
        // equipment slot itself so clients render the Flint counterpart in-hand
        // instead of only converting the eventual drop.
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        replaceMobEquipment(mob, EquipmentSlot.MAINHAND);
        replaceMobEquipment(mob, EquipmentSlot.OFFHAND);
    }

    @SubscribeEvent
    public static void onLivingEquipmentChange(LivingEquipmentChangeEvent event) {
        // Covers equipment assigned after joining the world, including mobs
        // picking up a disabled vanilla tool/weapon later. The next equipment
        // sync then exposes the Flint item visually to nearby clients.
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }

        ItemStack replacement = DisabledVanillaEquipment.replacementFor(event.getTo());
        if (replacement != event.getTo()) {
            mob.setItemSlot(event.getSlot(), replacement);
        }
    }

    @SubscribeEvent
    public static void onItemPickup(ItemEntityPickupEvent.Pre event) {
        // Safety net for item entities produced outside normal loot tables:
        // old-world drops, mod-added item entities, manual drops, etc.
        ItemEntity itemEntity = event.getItemEntity();
        replaceItemEntityStack(itemEntity);
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        // GLMs already convert normal loot-table drops. This additionally catches
        // drops another mod inserts directly into LivingDropsEvent.
        for (ItemEntity drop : event.getDrops()) {
            replaceItemEntityStack(drop);
        }
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (isRestrictedPlayer(event.getEntity())
                && DisabledVanillaEquipment.isDisabled(event.getEntity().getMainHandItem())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        cancelDisabledInteraction(event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        cancelDisabledInteraction(event);
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        cancelDisabledInteraction(event);
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isRestrictedPlayer(event.getEntity())
                && DisabledVanillaEquipment.isDisabled(event.getItemStack())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onFurnaceFuel(FurnaceFuelBurnTimeEvent event) {
        // A command/old-world wooden tool that still exists must remain inert in
        // automation contexts too while its tier is disabled.
        if (DisabledVanillaEquipment.isDisabled(event.getItemStack())) {
            event.setBurnTime(0);
        }
    }

    private static void replaceMobEquipment(Mob mob, EquipmentSlot slot) {
        ItemStack original = mob.getItemBySlot(slot);
        ItemStack replacement = DisabledVanillaEquipment.replacementFor(original);
        if (replacement != original) {
            mob.setItemSlot(slot, replacement);
        }
    }

    private static void replaceItemEntityStack(ItemEntity entity) {
        ItemStack original = entity.getItem();
        ItemStack replacement = DisabledVanillaEquipment.replacementFor(original);
        if (replacement != original) {
            entity.setItem(replacement);
        }
    }

    private static void cancelDisabledInteraction(PlayerInteractEvent event) {
        if (!isRestrictedPlayer(event.getEntity())
                || !DisabledVanillaEquipment.isDisabled(event.getItemStack())) {
            return;
        }

        if (event instanceof PlayerInteractEvent.RightClickItem rightClickItem) {
            rightClickItem.setCanceled(true);
        } else if (event instanceof PlayerInteractEvent.RightClickBlock rightClickBlock) {
            rightClickBlock.setCanceled(true);
        } else if (event instanceof PlayerInteractEvent.EntityInteract entityInteract) {
            entityInteract.setCanceled(true);
        }
    }

    private static boolean isRestrictedPlayer(Player player) {
        // Server-authoritative: avoid relying on a remote client's COMMON
        // config copy after an admin changes a toggle at runtime.
        return !player.level().isClientSide() && !player.getAbilities().instabuild;
    }
}
