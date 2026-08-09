package be.winnetrie.mod.simpleknapping.command;

import be.winnetrie.mod.simpleknapping.network.RecipeEditorNetwork;
import be.winnetrie.mod.simpleknapping.network.RecipeGuideNetwork;
import be.winnetrie.mod.simpleknapping.network.SettingsNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SimpleKnappingCommands {
    private SimpleKnappingCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("simpleknapping")
                        .then(Commands.literal("guide")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    RecipeGuideNetwork.openGuide(player);
                                    return 1;
                                }))
                        .then(Commands.literal("recipes")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    RecipeEditorNetwork.openEditor(player);
                                    return 1;
                                }))
                        .then(Commands.literal("settings")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    SettingsNetwork.openSettings(player);
                                    return 1;
                                }))
        );
    }

    public static boolean canEdit(ServerPlayer player) {
        return player.createCommandSourceStack().hasPermission(2);
    }
}
