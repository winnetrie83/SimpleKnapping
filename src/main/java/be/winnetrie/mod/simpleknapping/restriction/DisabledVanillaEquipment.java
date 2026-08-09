package be.winnetrie.mod.simpleknapping.restriction;

import be.winnetrie.mod.simpleknapping.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.Set;

/**
 * Central runtime gate for vanilla wooden and stone tools/weapons.
 *
 * Disabled tiers remain registered so commands and creative inventory keep
 * working normally. Their vanilla crafting outputs are hidden at runtime, and
 * normal survival acquisition routes replace the vanilla item with the closest
 * SimpleKnapping flint progression item instead.
 *
 * Any disabled vanilla item that still exists in a player's inventory (for
 * example via /give or an older world) remains covered by the inert-use guards.
 */
@SuppressWarnings("null")
public final class DisabledVanillaEquipment {
    private static final Set<ResourceLocation> WOODEN_IDS = ids(
            "minecraft:wooden_sword",
            "minecraft:wooden_pickaxe",
            "minecraft:wooden_axe",
            "minecraft:wooden_shovel",
            "minecraft:wooden_hoe"
    );

    private static final Set<ResourceLocation> STONE_IDS = ids(
            "minecraft:stone_sword",
            "minecraft:stone_pickaxe",
            "minecraft:stone_axe",
            "minecraft:stone_shovel",
            "minecraft:stone_hoe"
    );

    /**
     * Both wooden and stone tiers converge on the same flint progression.
     * Swords become the usable Flint Knife. The knife blade is only a crafting
     * component and must never be used as the progression replacement item.
     */
    private static final Map<ResourceLocation, ResourceLocation> FLINT_REPLACEMENTS = Map.ofEntries(
            Map.entry(ResourceLocation.parse("minecraft:wooden_sword"), ResourceLocation.parse("simpleknapping:flint_knife")),
            Map.entry(ResourceLocation.parse("minecraft:stone_sword"), ResourceLocation.parse("simpleknapping:flint_knife")),
            Map.entry(ResourceLocation.parse("minecraft:wooden_pickaxe"), ResourceLocation.parse("simpleknapping:flint_pickaxe")),
            Map.entry(ResourceLocation.parse("minecraft:stone_pickaxe"), ResourceLocation.parse("simpleknapping:flint_pickaxe")),
            Map.entry(ResourceLocation.parse("minecraft:wooden_axe"), ResourceLocation.parse("simpleknapping:flint_axe")),
            Map.entry(ResourceLocation.parse("minecraft:stone_axe"), ResourceLocation.parse("simpleknapping:flint_axe")),
            Map.entry(ResourceLocation.parse("minecraft:wooden_shovel"), ResourceLocation.parse("simpleknapping:flint_shovel")),
            Map.entry(ResourceLocation.parse("minecraft:stone_shovel"), ResourceLocation.parse("simpleknapping:flint_shovel")),
            Map.entry(ResourceLocation.parse("minecraft:wooden_hoe"), ResourceLocation.parse("simpleknapping:flint_hoe")),
            Map.entry(ResourceLocation.parse("minecraft:stone_hoe"), ResourceLocation.parse("simpleknapping:flint_hoe"))
    );

    private static volatile boolean runtimeStateInitialized;
    private static volatile boolean runtimeWoodenEnabled;
    private static volatile boolean runtimeStoneEnabled;

    private DisabledVanillaEquipment() {
    }

    /** Server startup/config changes and client sync packets all converge here. */
    public static void setRuntimeTierState(boolean woodenEnabled, boolean stoneEnabled) {
        runtimeWoodenEnabled = woodenEnabled;
        runtimeStoneEnabled = stoneEnabled;
        runtimeStateInitialized = true;
    }

    public static void syncRuntimeStateFromConfig() {
        setRuntimeTierState(
                Config.woodenToolsAndWeaponsEnabled(),
                Config.stoneToolsAndWeaponsEnabled()
        );
    }

    public static boolean woodenEnabled() {
        return runtimeStateInitialized ? runtimeWoodenEnabled : Config.woodenToolsAndWeaponsEnabled();
    }

    public static boolean stoneEnabled() {
        return runtimeStateInitialized ? runtimeStoneEnabled : Config.stoneToolsAndWeaponsEnabled();
    }

    public static boolean isDisabled(ItemStack stack) {
        return !stack.isEmpty() && isDisabled(stack.getItem());
    }

    public static boolean isDisabled(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return (!woodenEnabled() && WOODEN_IDS.contains(id))
                || (!stoneEnabled() && STONE_IDS.contains(id));
    }

    public static boolean isWooden(ItemStack stack) {
        return !stack.isEmpty() && WOODEN_IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isStone(ItemStack stack) {
        return !stack.isEmpty() && STONE_IDS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    /**
     * Returns the flint counterpart for a disabled vanilla stack. If the tier
     * is enabled, the stack is unrelated, or a replacement registry entry is
     * unexpectedly unavailable, the original stack instance is returned.
     *
     * Replacements intentionally start as clean SimpleKnapping items. Vanilla
     * damage/enchantment/components are not copied across item types because
     * they can be invalid or progression-breaking on a different item.
     */
    public static ItemStack replacementFor(ItemStack original) {
        if (!isDisabled(original)) {
            return original;
        }

        ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(original.getItem());
        ResourceLocation replacementId = FLINT_REPLACEMENTS.get(sourceId);
        if (replacementId == null || !BuiltInRegistries.ITEM.containsKey(replacementId)) {
            return original;
        }

        Item replacementItem = BuiltInRegistries.ITEM.get(replacementId);
        return new ItemStack(replacementItem, original.getCount());
    }

    public static boolean hasReplacement(ItemStack stack) {
        if (!isDisabled(stack)) {
            return false;
        }
        ResourceLocation sourceId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        ResourceLocation replacementId = FLINT_REPLACEMENTS.get(sourceId);
        return replacementId != null && BuiltInRegistries.ITEM.containsKey(replacementId);
    }

    private static Set<ResourceLocation> ids(String... values) {
        java.util.HashSet<ResourceLocation> ids = new java.util.HashSet<>();
        for (String value : values) {
            ids.add(ResourceLocation.parse(value));
        }
        return Set.copyOf(ids);
    }
}
