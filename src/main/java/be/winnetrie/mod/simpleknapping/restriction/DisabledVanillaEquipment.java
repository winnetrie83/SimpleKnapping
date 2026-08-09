package be.winnetrie.mod.simpleknapping.restriction;

import be.winnetrie.mod.simpleknapping.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
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
public final class DisabledVanillaEquipment {
    private static final Set<Identifier> WOODEN_IDS = ids(
            "minecraft:wooden_sword",
            "minecraft:wooden_pickaxe",
            "minecraft:wooden_axe",
            "minecraft:wooden_shovel",
            "minecraft:wooden_hoe",
            "minecraft:wooden_spear"
    );

    private static final Set<Identifier> STONE_IDS = ids(
            "minecraft:stone_sword",
            "minecraft:stone_pickaxe",
            "minecraft:stone_axe",
            "minecraft:stone_shovel",
            "minecraft:stone_hoe",
            "minecraft:stone_spear"
    );

    /**
     * Both wooden and stone tiers converge on the same flint progression.
     * Swords become the usable Flint Knife. The knife blade is only a crafting
     * component and must never be used as the progression replacement item.
     */
    private static final Map<Identifier, Identifier> FLINT_REPLACEMENTS = Map.ofEntries(
            Map.entry(Identifier.parse("minecraft:wooden_sword"), Identifier.parse("simpleknapping:flint_knife")),
            Map.entry(Identifier.parse("minecraft:stone_sword"), Identifier.parse("simpleknapping:flint_knife")),
            Map.entry(Identifier.parse("minecraft:wooden_pickaxe"), Identifier.parse("simpleknapping:flint_pickaxe")),
            Map.entry(Identifier.parse("minecraft:stone_pickaxe"), Identifier.parse("simpleknapping:flint_pickaxe")),
            Map.entry(Identifier.parse("minecraft:wooden_axe"), Identifier.parse("simpleknapping:flint_axe")),
            Map.entry(Identifier.parse("minecraft:stone_axe"), Identifier.parse("simpleknapping:flint_axe")),
            Map.entry(Identifier.parse("minecraft:wooden_shovel"), Identifier.parse("simpleknapping:flint_shovel")),
            Map.entry(Identifier.parse("minecraft:stone_shovel"), Identifier.parse("simpleknapping:flint_shovel")),
            Map.entry(Identifier.parse("minecraft:wooden_hoe"), Identifier.parse("simpleknapping:flint_hoe")),
            Map.entry(Identifier.parse("minecraft:stone_hoe"), Identifier.parse("simpleknapping:flint_hoe")),
            Map.entry(Identifier.parse("minecraft:wooden_spear"), Identifier.parse("simpleknapping:flint_spear")),
            Map.entry(Identifier.parse("minecraft:stone_spear"), Identifier.parse("simpleknapping:flint_spear"))
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
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null) {
            return false;
        }
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

        Identifier sourceId = BuiltInRegistries.ITEM.getKey(original.getItem());
        Identifier replacementId = FLINT_REPLACEMENTS.get(sourceId);
        if (replacementId == null || !BuiltInRegistries.ITEM.containsKey(replacementId)) {
            return original;
        }

        Item replacementItem = BuiltInRegistries.ITEM.getValue(replacementId);
        return new ItemStack(replacementItem, original.getCount());
    }

    public static boolean hasReplacement(ItemStack stack) {
        if (!isDisabled(stack)) {
            return false;
        }
        Identifier sourceId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Identifier replacementId = FLINT_REPLACEMENTS.get(sourceId);
        return replacementId != null && BuiltInRegistries.ITEM.containsKey(replacementId);
    }

    private static Set<Identifier> ids(String... values) {
        java.util.HashSet<Identifier> ids = new java.util.HashSet<>();
        for (String value : values) {
            ids.add(Identifier.parse(value));
        }
        return Set.copyOf(ids);
    }
}
