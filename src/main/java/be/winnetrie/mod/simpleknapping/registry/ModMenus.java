package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Menu registration. The opening payload carries the full knapping type so custom world types work on clients. */
@SuppressWarnings("null")
public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SimpleKnapping.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<KnappingMenu>> KNAPPING_MENU =
            MENUS.register("knapping_menu", () ->
                    IMenuTypeExtension.create((containerId, inventory, buffer) -> {
                        ResourceLocation typeId = buffer.readResourceLocation();
                        ResourceLocation toolId = buffer.readResourceLocation();
                        ResourceLocation materialId = buffer.readResourceLocation();
                        int materialCost = buffer.readVarInt();
                        ResourceLocation texture = buffer.readResourceLocation();
                        ResourceLocation textureBlock = buffer.readResourceLocation();
                        ResourceLocation inputMaterialId = buffer.readResourceLocation();

                        Item tool = BuiltInRegistries.ITEM.get(toolId);
                        Item material = BuiltInRegistries.ITEM.get(materialId);
                        Item inputMaterial = BuiltInRegistries.ITEM.get(inputMaterialId);
                        KnappingType type = new KnappingType(
                                typeId,
                                tool,
                                material,
                                materialCost,
                                texture,
                                textureBlock
                        );
                        return new KnappingMenu(containerId, inventory, type, inputMaterial);
                    })
            );
}
