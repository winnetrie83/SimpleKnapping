package be.winnetrie.mod.simpleknapping.loot;

import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import be.winnetrie.mod.simpleknapping.registry.ModLootModifiers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

/**
 * Progression-aware global loot modifier.
 *
 * When the wooden and/or stone tier is disabled, generated vanilla equipment
 * is replaced in-place with its SimpleKnapping flint counterpart instead of
 * simply being deleted from loot. This covers generated chest loot, fishing,
 * entity loot tables and other normal loot-table based sources.
 */
public class RemoveToolLootModifier extends LootModifier {
    public static final MapCodec<RemoveToolLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, (conditions, priority) ->
                    new RemoveToolLootModifier(conditions, priority)));

    public RemoveToolLootModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (int index = 0; index < generatedLoot.size(); index++) {
            ItemStack original = generatedLoot.get(index);
            ItemStack replacement = DisabledVanillaEquipment.replacementFor(original);
            if (replacement != original) {
                generatedLoot.set(index, replacement);
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.REMOVE_TOOL_LOOT.get();
    }
}
