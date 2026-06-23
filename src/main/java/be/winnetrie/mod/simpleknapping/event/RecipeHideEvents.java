package be.winnetrie.mod.simpleknapping.event;

import be.winnetrie.mod.simpleknapping.Config;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

public class RecipeHideEvents {

    @SubscribeEvent
    public static void onModifyRecipeJsons(ModifyRecipeJsonsEvent event) {

        if (!Config.ENABLE_PROGRESSION.get()) {
            return;
        }

        if (!Config.ENABLE_TOOL_RECIPE_LOCKS.get()) {
            return;
        }

        if (Config.HIDE_WOODEN_TOOL_RECIPES.get()) {
            removeRecipe(event, "minecraft:wooden_sword");
            removeRecipe(event, "minecraft:wooden_pickaxe");
            removeRecipe(event, "minecraft:wooden_axe");
            removeRecipe(event, "minecraft:wooden_shovel");
            removeRecipe(event, "minecraft:wooden_hoe");
            removeRecipe(event, "minecraft:wooden_spear");
        }

        if (Config.HIDE_STONE_TOOL_RECIPES.get()) {
            removeRecipe(event, "minecraft:stone_sword");
            removeRecipe(event, "minecraft:stone_pickaxe");
            removeRecipe(event, "minecraft:stone_axe");
            removeRecipe(event, "minecraft:stone_shovel");
            removeRecipe(event, "minecraft:stone_hoe");
            removeRecipe(event, "minecraft:stone_spear");
        }
    }

    private static void removeRecipe(ModifyRecipeJsonsEvent event, String id) {
        event.getRecipeJsons().remove(Identifier.parse(id));
    }
}