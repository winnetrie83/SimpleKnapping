package be.winnetrie.mod.simpleknapping;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_PROGRESSION = BUILDER
        .comment("Enable or disable all Simple Knapping progression systems.")
        .define("enable_progression", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PLANT_FIBER_DROPS = BUILDER
        .comment("Enable plant fiber drops from short grass and tall grass.")
        .define("enable_plant_fiber_drops", true);

    public static final ModConfigSpec.BooleanValue ENABLE_TOOL_RECIPE_LOCKS = BUILDER
        .comment("Hide vanilla wooden and stone tool recipes when progression is enabled.")
        .define("enableToolRecipeLocks", true);

    public static final ModConfigSpec.BooleanValue HIDE_WOODEN_TOOL_RECIPES = BUILDER
        .define("hideWoodenToolRecipes", true);

    public static final ModConfigSpec.BooleanValue HIDE_STONE_TOOL_RECIPES = BUILDER
        .define("hideStoneToolRecipes", true);

    public static final ModConfigSpec.BooleanValue REQUIRE_AXE_FOR_LOGS = BUILDER
        .comment("Require an axe to break logs.")
        .define("requireAxeForLogs", true);    

    public static final ModConfigSpec.BooleanValue ENABLE_STICK_DROPS = BUILDER
        .comment("Allow leaves to drop sticks.")
        .define("enableStickDrops", true);

    static final ModConfigSpec SPEC = BUILDER.build();
}