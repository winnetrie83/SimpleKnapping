package be.winnetrie.mod.simpleknapping;

import be.winnetrie.mod.simpleknapping.client.screen.KnappingScreen;
import be.winnetrie.mod.simpleknapping.registry.ModMenus;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = SimpleKnapping.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = SimpleKnapping.MODID, value = Dist.CLIENT)
public class SimpleKnappingClient {

    public SimpleKnappingClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onRegisterMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.KNAPPING_MENU.get(), KnappingScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        SimpleKnapping.LOGGER.info("HELLO FROM CLIENT SETUP");
        SimpleKnapping.LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
    }
}