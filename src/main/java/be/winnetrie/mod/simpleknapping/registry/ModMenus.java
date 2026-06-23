package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.knapping.KnappingType;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.menu.KnappingMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, SimpleKnapping.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<KnappingMenu>> KNAPPING_MENU =
            MENUS.register("knapping_menu", () ->
                    IMenuTypeExtension.create((containerId, inventory, buffer) -> {

                        Identifier typeId = buffer.readIdentifier();
                        KnappingType type = KnappingTypeManager.get(typeId);

                        return new KnappingMenu(containerId, inventory, type);
                    })
            );
}