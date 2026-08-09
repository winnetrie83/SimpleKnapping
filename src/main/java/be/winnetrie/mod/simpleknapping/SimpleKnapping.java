package be.winnetrie.mod.simpleknapping;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import be.winnetrie.mod.simpleknapping.command.SimpleKnappingCommands;
import be.winnetrie.mod.simpleknapping.event.DisabledEquipmentEvents;
import be.winnetrie.mod.simpleknapping.event.KnappingInteractionEvents;
import be.winnetrie.mod.simpleknapping.event.PlantFiberEvents;
import be.winnetrie.mod.simpleknapping.event.StickDropEvents;
import be.winnetrie.mod.simpleknapping.event.ToolBreakEvents;
import be.winnetrie.mod.simpleknapping.knapping.CustomKnappingRecipeData;
import be.winnetrie.mod.simpleknapping.knapping.CustomKnappingTypeData;
import be.winnetrie.mod.simpleknapping.knapping.KnappingRecipeManager;
import be.winnetrie.mod.simpleknapping.knapping.KnappingTypeManager;
import be.winnetrie.mod.simpleknapping.network.RecipeEditorNetwork;
import be.winnetrie.mod.simpleknapping.network.SettingsNetwork;
import be.winnetrie.mod.simpleknapping.restriction.DisabledVanillaEquipment;
import be.winnetrie.mod.simpleknapping.registry.ModCreativeTabs;
import be.winnetrie.mod.simpleknapping.registry.ModItems;
import be.winnetrie.mod.simpleknapping.registry.ModLootModifiers;
import be.winnetrie.mod.simpleknapping.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

@SuppressWarnings("null")
@Mod(SimpleKnapping.MODID)
public class SimpleKnapping {

    public static final String MODID = "simpleknapping";
    public static final Logger LOGGER = LogUtils.getLogger();

    public SimpleKnapping(IEventBus modEventBus, ModContainer modContainer) {

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModLootModifiers.LOOT_MODIFIERS.register(modEventBus);

        // Common/network registration belongs on the mod event bus.
        modEventBus.addListener(RecipeEditorNetwork::registerPayloads);

        NeoForge.EVENT_BUS.register(DisabledEquipmentEvents.class);
        NeoForge.EVENT_BUS.register(KnappingInteractionEvents.class);
        NeoForge.EVENT_BUS.register(PlantFiberEvents.class);
        NeoForge.EVENT_BUS.register(StickDropEvents.class);
        NeoForge.EVENT_BUS.register(ToolBreakEvents.class);

        NeoForge.EVENT_BUS.addListener(this::addReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onServerStarted);
        NeoForge.EVENT_BUS.addListener(SimpleKnappingCommands::register);
        NeoForge.EVENT_BUS.addListener(SettingsNetwork::onPlayerLoggedIn);

        LOGGER.info("Simple Knapping loaded and ready.");
        LOGGER.info("Thank you for using Simple Knapping!");
    }

    private void addReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new KnappingTypeManager());
        event.addListener(new KnappingRecipeManager());
    }

    private void onServerStarted(ServerStartedEvent event) {
        DisabledVanillaEquipment.syncRuntimeStateFromConfig();
        // Types must be restored before recipes so custom recipe type ids are live immediately.
        CustomKnappingTypeData.get(event.getServer());
        CustomKnappingRecipeData.get(event.getServer());
    }
}
