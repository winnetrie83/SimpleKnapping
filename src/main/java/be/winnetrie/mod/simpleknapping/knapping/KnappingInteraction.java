package be.winnetrie.mod.simpleknapping.knapping;

import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared opening logic for built-in, vanilla and modded knapping tools. */
public final class KnappingInteraction {
    private KnappingInteraction() {
    }

    /** Match the actual tool and recipe material; reject ambiguous pairs. */
    public static KnappingType findType(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return null;
        }

        ItemStack toolStack = player.getMainHandItem();
        ItemStack materialStack = player.getOffhandItem();
        if (toolStack.isEmpty() || materialStack.isEmpty()) {
            return null;
        }

        if (!player.hasInfiniteMaterials() && DisabledVanillaEquipment.isDisabled(toolStack)) {
            return null;
        }

        Item tool = toolStack.getItem();
        Item material = materialStack.getItem();
        KnappingType match = null;
        for (KnappingType type : KnappingTypeManager.getEffectiveTypes().values()) {
            if (type.tool() != tool || !KnappingRecipeManager.hasActiveRecipeForMaterial(type.id(), material)) {
                continue;
            }
            if (match != null && !match.id().equals(type.id())) {
                return null;
            }
            match = type;
        }
        return match;
    }

    /** Material is consumed only when a completed recipe is taken. */
    public static boolean tryOpen(ServerPlayer serverPlayer, InteractionHand hand) {
        KnappingType type = findType(serverPlayer, hand);
        if (type == null) {
            return false;
        }

        ItemStack offhandStack = serverPlayer.getOffhandItem();
        int minimumCost = KnappingRecipeManager.minimumMaterialCost(type.id(), offhandStack.getItem());
        if (minimumCost <= 0) {
            return false;
        }
        if (offhandStack.getCount() < minimumCost && !serverPlayer.hasInfiniteMaterials()) {
            serverPlayer.sendSystemMessage(Component.literal("Need at least " + minimumCost + " x "
                    + offhandStack.getHoverName().getString() + " to carve/knap with this material."));
            return true;
        }

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, player) -> {
                            KnappingMenu menu = new KnappingMenu(containerId, inventory, type, offhandStack.getItem());
                            // Apply server config and player's game mode before initial slot synchronization.
                            menu.refreshPreviewPermission(player);
                            return menu;
                        },
                        Component.literal(formatTitle(type.id().getPath()))
                ),
                buffer -> {
                    buffer.writeIdentifier(type.id());
                    buffer.writeIdentifier(BuiltInRegistries.ITEM.getKey(type.tool()));
                    buffer.writeIdentifier(BuiltInRegistries.ITEM.getKey(type.material()));
                    buffer.writeVarInt(type.materialCost());
                    buffer.writeIdentifier(type.texture());
                    buffer.writeIdentifier(type.textureBlock());
                    buffer.writeIdentifier(BuiltInRegistries.ITEM.getKey(offhandStack.getItem()));
                }
        );
        return true;
    }

    private static String formatTitle(String path) {
        String[] parts = path.split("_");
        StringBuilder title = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                title.append(Character.toUpperCase(part.charAt(0)));
                title.append(part.substring(1));
                title.append(' ');
            }
        }
        return title.toString().trim();
    }
}
