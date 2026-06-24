package be.winnetrie.mod.simpleknapping.loot;

import be.winnetrie.mod.simpleknapping.Config;
import be.winnetrie.mod.simpleknapping.registry.ModItemTags;
import be.winnetrie.mod.simpleknapping.registry.ModLootModifiers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class RemoveToolLootModifier extends LootModifier {

    public static final MapCodec<RemoveToolLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
        codecStart(instance).apply(instance, (conditions, priority) ->
            new RemoveToolLootModifier(conditions, priority)));

    public RemoveToolLootModifier(LootItemCondition[] conditions, int priority) {
        super(conditions, priority);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {

        if (!Config.REMOVE_WOODEN_AND_STONE_TOOLS_FROM_LOOT.get()) {
            return generatedLoot;
        }

        generatedLoot.removeIf(stack -> stack.is(ModItemTags.REMOVED_FROM_LOOT));

        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return ModLootModifiers.REMOVE_TOOL_LOOT.get();
    }
}