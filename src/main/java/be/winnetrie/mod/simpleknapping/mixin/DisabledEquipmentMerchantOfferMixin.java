package be.winnetrie.mod.simpleknapping.mixin;

import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rewrites disabled vanilla villager/wandering-trader outputs at runtime.
 *
 * Hooking both getResult() and assemble() makes the replacement visible in the
 * merchant offer list and authoritative in the actual trade result, while the
 * underlying vanilla offer can remain unchanged and becomes original again as
 * soon as the corresponding tier is re-enabled.
 */
@Mixin(MerchantOffer.class)
public abstract class DisabledEquipmentMerchantOfferMixin {

    @Inject(method = "getResult", at = @At("RETURN"), cancellable = true)
    private void simpleknapping$replaceDisplayedTradeResult(CallbackInfoReturnable<ItemStack> cir) {
        replaceResult(cir);
    }

    @Inject(method = "assemble", at = @At("RETURN"), cancellable = true)
    private void simpleknapping$replaceAssembledTradeResult(CallbackInfoReturnable<ItemStack> cir) {
        replaceResult(cir);
    }

    private static void replaceResult(CallbackInfoReturnable<ItemStack> cir) {
        ItemStack original = cir.getReturnValue();
        ItemStack replacement = DisabledVanillaEquipment.replacementFor(original);
        if (replacement != original) {
            cir.setReturnValue(replacement);
        }
    }
}
