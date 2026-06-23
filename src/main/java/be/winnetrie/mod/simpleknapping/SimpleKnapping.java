package be.winnetrie.mod.simpleknapping;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;


import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.registry.ModCreativeTabs;
import be.winnetrie.mod.simpleknapping.registry.ModItems;
import be.winnetrie.mod.simpleknapping.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.minecraft.resources.Identifier;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.event.PlantFiberEvents;

@Mod(SimpleKnapping.MODID)
public class SimpleKnapping {

    public static final String MODID = "simpleknapping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleKnapping(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);

        NeoForge.EVENT_BUS.register(PlantFiberEvents.class);
       
        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);

        LOGGER.info("Simple Knapping loaded");
    }

    private void addReloadListeners(AddServerReloadListenersEvent event) {

        event.addListener(
                Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "knapping_types"),
                new KnappingTypeManager()
        );

        event.addListener(
                Identifier.fromNamespaceAndPath(SimpleKnapping.MODID, "knapping_recipes"),
                new KnappingRecipeManager()
        );
    }
}