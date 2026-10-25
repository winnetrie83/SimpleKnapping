package be.winnetrie.mod.simpleknapping;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_PLANT_FIBER_DROPS = BUILDER
            .comment("Enable plant fiber drops from short grass and tall grass.")
            .define("enable_plant_fiber_drops", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TOOL_RECIPE_LOCKS = BUILDER
            .comment("Legacy master switch kept for config compatibility. The admin GUI now controls wooden/stone tiers independently.")
            .define("enableToolRecipeLocks", true);

    public static final ModConfigSpec.BooleanValue HIDE_WOODEN_TOOL_RECIPES = BUILDER
            .comment("When true, vanilla wooden tools/weapons are disabled; normal survival sources replace them with flint equivalents.")
            .define("hideWoodenToolRecipes", true);

    public static final ModConfigSpec.BooleanValue HIDE_STONE_TOOL_RECIPES = BUILDER
            .comment("When true, vanilla stone tools/weapons are disabled; normal survival sources replace them with flint equivalents.")
            .define("hideStoneToolRecipes", true);

    public static final ModConfigSpec.BooleanValue REMOVE_WOODEN_AND_STONE_TOOLS_FROM_LOOT = BUILDER
            .comment("Legacy combined loot switch kept for config compatibility. Loot replacement now follows independent toggles.")
            .define("remove_wooden_and_stone_tools_from_loot", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_AXE_FOR_LOGS = BUILDER
            .comment("Require an axe to break logs; exposed as the inverse Tree punching setting.")
            .define("requireAxeForLogs", true);

    public static final ModConfigSpec.BooleanValue ENABLE_STICK_DROPS = BUILDER
            .comment("Allow leaves to drop sticks.")
            .define("enableStickDrops", true);

    public static final ModConfigSpec.DoubleValue KNAPPING_TOOL_DAMAGE_CHANCE = BUILDER
            .comment("Probability (0.0 to 1.0) of losing one durability for each removed knapping tile. Default 0.33.")
            .defineInRange("knappingToolDamageChance", 0.33D, 0.0D, 1.0D);

    public static final ModConfigSpec.BooleanValue ENABLE_SURVIVAL_RECIPE_PREVIEW = BUILDER
            .comment("Allow the knapping recipe preview in Survival/Adventure mode. Disabled by default.",
                    "Creative mode always has access to recipe previews regardless of this option.",
                    "This permission is enforced by the server and synchronized with each knapping menu.")
            .define("enableSurvivalRecipePreview", false);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() { }

    public static boolean woodenToolsAndWeaponsEnabled() { return !HIDE_WOODEN_TOOL_RECIPES.get(); }
    public static boolean stoneToolsAndWeaponsEnabled() { return !HIDE_STONE_TOOL_RECIPES.get(); }
    public static boolean treePunchingEnabled() { return !REQUIRE_AXE_FOR_LOGS.get(); }

    public static void setWoodenToolsAndWeaponsEnabled(boolean enabled) {
        HIDE_WOODEN_TOOL_RECIPES.set(!enabled);
        HIDE_WOODEN_TOOL_RECIPES.save();
    }

    public static void setStoneToolsAndWeaponsEnabled(boolean enabled) {
        HIDE_STONE_TOOL_RECIPES.set(!enabled);
        HIDE_STONE_TOOL_RECIPES.save();
    }

    public static void setTreePunchingEnabled(boolean enabled) {
        REQUIRE_AXE_FOR_LOGS.set(!enabled);
        REQUIRE_AXE_FOR_LOGS.save();
    }
}
