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
@SuppressWarnings("null")
public final class KnappingInteraction {
    private KnappingInteraction() {
    }

    /**
     * A type is now selected by the held tool plus the material used by one of
     * that type's active recipes. The material is no longer fixed per type.
     */
    public static KnappingType findType(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return null;
        }

        ItemStack toolStack = player.getMainHandItem();
        ItemStack materialStack = player.getOffhandItem();
        if (toolStack.isEmpty() || materialStack.isEmpty()) {
            return null;
        }

        // Disabled vanilla equipment must not regain functionality by being
        // configured as a custom knapping/carving tool. Creative is exempt.
        if (!player.getAbilities().instabuild && DisabledVanillaEquipment.isDisabled(toolStack)) {
            return null;
        }

        Item tool = toolStack.getItem();
        Item material = materialStack.getItem();
        KnappingType match = null;

        for (KnappingType type : KnappingTypeManager.getEffectiveTypes().values()) {
            if (type.tool() != tool || !KnappingRecipeManager.hasActiveRecipeForMaterial(type.id(), material)) {
                continue;
            }

            // Ambiguous resource/datapack configurations are deliberately not opened.
            // The in-game editor prevents creating this situation for server recipes.
            if (match != null && !match.id().equals(type.id())) {
                return null;
            }
            match = type;
        }

        return match;
    }

    /**
     * Returns true when the held tool/material pair belongs to a knapping type.
     * Material is consumed only when a finished recipe is taken from the result
     * slot, because each recipe can now require a different amount.
     */
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

        if (offhandStack.getCount() < minimumCost && !serverPlayer.getAbilities().instabuild) {
            serverPlayer.sendSystemMessage(
                    Component.literal("Need at least " + minimumCost + " x "
                            + offhandStack.getHoverName().getString() + " to carve/knap with this material.")
            );
            return true;
        }

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, player) -> new KnappingMenu(containerId, inventory, type, offhandStack.getItem()),
                        Component.literal(formatTitle(type.id().getPath()))
                ),
                buffer -> {
                    buffer.writeResourceLocation(type.id());
                    buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(type.tool()));
                    buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(type.material()));
                    buffer.writeVarInt(type.materialCost());
                    buffer.writeResourceLocation(type.texture());
                    buffer.writeResourceLocation(type.textureBlock());
                    buffer.writeResourceLocation(BuiltInRegistries.ITEM.getKey(offhandStack.getItem()));
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
