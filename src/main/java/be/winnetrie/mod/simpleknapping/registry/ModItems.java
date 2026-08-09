package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;
import be.winnetrie.mod.simpleknapping.item.KnappingToolItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@SuppressWarnings("null")
public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleKnapping.MODID);

    public static final DeferredItem<Item> FLINT_KNAPPING_TOOL = ITEMS.registerItem(
            "flint_knapping_tool",
            p -> new KnappingToolItem(p.durability(64).stacksTo(1))
    );

    public static final DeferredItem<Item> FLINT_AXE = ITEMS.registerItem(
            "flint_axe",
            p -> new AxeItem(
                    ModToolMaterials.FLINT,
                    p.attributes(AxeItem.createAttributes(ModToolMaterials.FLINT, 6.0F, -3.2F))
            )
    );
    public static final DeferredItem<Item> FLINT_AXE_HEAD = ITEMS.registerSimpleItem("flint_axe_head", new Item.Properties());

    public static final DeferredItem<Item> FLINT_HOE = ITEMS.registerItem(
            "flint_hoe",
            p -> new HoeItem(
                    ModToolMaterials.FLINT,
                    p.attributes(HoeItem.createAttributes(ModToolMaterials.FLINT, 0.0F, -3.0F))
            )
    );
    public static final DeferredItem<Item> FLINT_HOE_HEAD = ITEMS.registerSimpleItem("flint_hoe_head", new Item.Properties());

    public static final DeferredItem<Item> FLINT_PICKAXE = ITEMS.registerItem(
            "flint_pickaxe",
            p -> new PickaxeItem(
                    ModToolMaterials.FLINT,
                    p.attributes(PickaxeItem.createAttributes(ModToolMaterials.FLINT, 1.0F, -2.8F))
            )
    );
    public static final DeferredItem<Item> FLINT_PICKAXE_HEAD = ITEMS.registerSimpleItem("flint_pickaxe_head", new Item.Properties());

    public static final DeferredItem<Item> FLINT_SHOVEL = ITEMS.registerItem(
            "flint_shovel",
            p -> new ShovelItem(
                    ModToolMaterials.FLINT,
                    p.attributes(ShovelItem.createAttributes(ModToolMaterials.FLINT, 1.5F, -3.0F))
            )
    );
    public static final DeferredItem<Item> FLINT_SHOVEL_HEAD = ITEMS.registerSimpleItem("flint_shovel_head", new Item.Properties());

    public static final DeferredItem<Item> PLANT_FIBER = ITEMS.registerSimpleItem("plant_fiber", new Item.Properties());
    public static final DeferredItem<Item> PLANT_FIBER_BUNDLE = ITEMS.registerSimpleItem("plant_fiber_bundle", new Item.Properties());
    public static final DeferredItem<Item> STRAW_BUNDLE = ITEMS.registerSimpleItem("straw_bundle", new Item.Properties());

    public static final DeferredItem<Item> FLINT_KNIFE = ITEMS.registerItem(
            "flint_knife",
            p -> new SwordItem(
                    ModToolMaterials.FLINT_KNIFE,
                    p.attributes(SwordItem.createAttributes(ModToolMaterials.FLINT_KNIFE, 1.5F, -1.5F))
            )
    );
    public static final DeferredItem<Item> FLINT_KNIFE_BLADE = ITEMS.registerSimpleItem("flint_knife_blade", new Item.Properties());
}
