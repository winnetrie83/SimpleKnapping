package be.winnetrie.mod.simpleknapping.item;

import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class KnappingToolItem extends Item {

    public KnappingToolItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        ItemStack offhandStack = player.getOffhandItem();
        KnappingType type = KnappingTypeManager.getByMaterial(offhandStack.getItem());

        if (type == null) {
            return InteractionResult.PASS;
        }

        if (offhandStack.getCount() < type.materialCost() && !player.hasInfiniteMaterials()) {
            return InteractionResult.FAIL;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }

        if (!serverPlayer.hasInfiniteMaterials()) {
            offhandStack.shrink(type.materialCost());

            if (offhandStack.isEmpty()) {
                serverPlayer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
            } 
            else {
                serverPlayer.setItemInHand(InteractionHand.OFF_HAND, offhandStack);
            }

            serverPlayer.getInventory().setChanged();
            serverPlayer.inventoryMenu.broadcastChanges();
        }

        serverPlayer.openMenu(
                new SimpleMenuProvider(
                        (containerId, inventory, p) -> new KnappingMenu(containerId, inventory, type),
                        Component.literal(formatTitle(type.id().getPath()) + " Knapping")
                ),
                buffer -> buffer.writeIdentifier(type.id())
        );

        return InteractionResult.SUCCESS;
    }

    private String formatTitle(String path) {

        String[] parts = path.split("_");
        StringBuilder title = new StringBuilder();

        for (String part : parts) {
            if (!part.isEmpty()) {
                title.append(Character.toUpperCase(part.charAt(0)));
                title.append(part.substring(1));
                title.append(" ");
            }
        }

        return title.toString().trim();
    }
}