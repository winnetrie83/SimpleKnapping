package be.winnetrie.mod.simpleknapping.mixin;

import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Runtime slot behavior for disabled vanilla equipment.
 *
 * - Vanilla crafting ResultSlots remain visually EMPTY while the relevant tier
 *   is disabled, preserving instant toggling without a resource reload.
 * - Non-player containers show disabled wooden/stone stacks as their flint
 *   counterpart. This includes chest/container slots and merchant result slots.
 * - Before such a stack is actually removed, the authoritative underlying
 *   container is converted too, preventing client-only display substitutions.
 * - The player's own inventory is deliberately excluded so /give and Creative
 *   can still provide the original vanilla item; inert-use guards handle those.
 */
@SuppressWarnings("null")
@Mixin(Slot.class)
public abstract class DisabledEquipmentSlotMixin {
    @Shadow @Final public Container container;

    @Inject(method = "getItem", at = @At("RETURN"), cancellable = true)
    private void simpleknapping$rewriteDisplayedDisabledEquipment(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack shown = cir.getReturnValue();
        if (!DisabledVanillaEquipment.isDisabled(shown)) {
            return;
        }

        if ((Object) this instanceof ResultSlot) {
            // Crafting/similar result recipes are gated, not auto-converted.
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }

        if (this.container instanceof Inventory) {
            // Preserve command/creative-created originals in player inventory.
            return;
        }

        ItemStack replacement = DisabledVanillaEquipment.replacementFor(shown);
        if (replacement != shown) {
            cir.setReturnValue(replacement);
        }
    }

    @Inject(method = "mayPickup", at = @At("HEAD"), cancellable = true)
    private void simpleknapping$prepareDisabledEquipmentPickup(Player player, CallbackInfoReturnable<Boolean> cir) {
        // Only mutate the authoritative server container. Client rendering is
        // handled independently by getItem above.
        if (!(player instanceof ServerPlayer)) {
            return;
        }

        Slot self = (Slot) (Object) this;
        ItemStack raw = this.container.getItem(self.getSlotIndex());
        if (!DisabledVanillaEquipment.isDisabled(raw)) {
            return;
        }

        if ((Object) this instanceof ResultSlot) {
            // Never let a disabled vanilla crafting result be taken.
            cir.setReturnValue(false);
            return;
        }

        if (this.container instanceof Inventory) {
            // /give + Creative inventory originals may be moved around normally;
            // their gameplay actions remain inert for survival players.
            return;
        }

        ItemStack replacement = DisabledVanillaEquipment.replacementFor(raw);
        if (replacement != raw) {
            self.set(replacement);
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void simpleknapping$replaceBeforeContainerExtraction(int amount, CallbackInfoReturnable<ItemStack> cir) {
        if ((Object) this instanceof ResultSlot || this.container instanceof Inventory) {
            return;
        }

        Slot self = (Slot) (Object) this;
        ItemStack raw = this.container.getItem(self.getSlotIndex());
        ItemStack replacement = DisabledVanillaEquipment.replacementFor(raw);
        if (replacement != raw) {
            self.set(replacement);
        }
    }
}
