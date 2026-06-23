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

    static final ModConfigSpec SPEC = BUILDER.build();
}