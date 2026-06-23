package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SimpleKnapping.MODID);

    public static final Supplier<CreativeModeTab> simpleknapping_TAB = CREATIVE_TABS.register("simpleknapping_tab", () -> CreativeModeTab.builder()
            .title(Component.literal("Simple Knapping"))
            .icon(() -> ModItems.FLINT_KNAPPING_TOOL.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                
                output.accept(ModItems.FLINT_KNAPPING_TOOL.get());
                output.accept(ModItems.FLINT_AXE.get());
                output.accept(ModItems.FLINT_AXE_HEAD.get());
                output.accept(ModItems.FLINT_HOE.get());
                output.accept(ModItems.FLINT_HOE_HEAD.get());
                output.accept(ModItems.FLINT_PICKAXE.get());
                output.accept(ModItems.FLINT_PICKAXE_HEAD.get());
                output.accept(ModItems.FLINT_SHOVEL.get());
                output.accept(ModItems.FLINT_SHOVEL_HEAD.get());

                
                output.accept(ModItems.PLANT_FIBER.get());
                output.accept(ModItems.PLANT_FIBER_BUNDLE.get());
                output.accept(ModItems.STRAW_BUNDLE.get());
                output.accept(ModItems.FLINT_KNIFE.get());
                output.accept(ModItems.FLINT_KNIFE_BLADE.get());
                output.accept(ModItems.FLINT_SPEAR.get());
                output.accept(ModItems.FLINT_SPEAR_HEAD.get());
                
                
            })
            .build());
}