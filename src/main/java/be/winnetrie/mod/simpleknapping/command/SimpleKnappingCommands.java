package be.winnetrie.mod.simpleknapping.command;

import be.winnetrie.mod.simpleknapping.network.RecipeEditorNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionCheck;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class SimpleKnappingCommands {
    private SimpleKnappingCommands() {
    }

    /** Gamemaster is the vanilla permission intended for world-editing/admin commands. */
    public static final PermissionCheck RECIPE_EDITOR_PERMISSION =
            new PermissionCheck.Require(Permissions.COMMANDS_GAMEMASTER);

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("simpleknapping")
                        .then(Commands.literal("recipes")
                                .requires(Commands.hasPermission(RECIPE_EDITOR_PERMISSION))
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayerOrException();
                                    RecipeEditorNetwork.openEditor(player);
                                    return 1;
                                }))
        );
    }

    public static boolean canEdit(ServerPlayer player) {
        return RECIPE_EDITOR_PERMISSION.check(player.permissions());
    }
}
