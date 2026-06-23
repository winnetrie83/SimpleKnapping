package be.winnetrie.mod.simpleknapping.registry;

import be.winnetrie.mod.simpleknapping.SimpleKnapping;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.ToolMaterial;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import be.winnetrie.mod.simpleknapping.item.KnappingToolItem;

public class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SimpleKnapping.MODID);

    

    public static final DeferredItem<Item> FLINT_KNAPPING_TOOL = ITEMS.registerItem("flint_knapping_tool", p -> new KnappingToolItem(p.durability(64).stacksTo(1)));


    public static final DeferredItem<Item> FLINT_AXE = ITEMS.registerItem("flint_axe", p -> new AxeItem(ToolMaterial.WOOD, 6.0F, -3.2F, p.durability(96)));

    public static final DeferredItem<Item> FLINT_AXE_HEAD = ITEMS.registerSimpleItem("flint_axe_head", () -> new Item.Properties());

    public static final DeferredItem<Item> FLINT_HOE = ITEMS.registerItem("flint_hoe", p -> new HoeItem(ToolMaterial.WOOD, 0.0F, -3.0F, p.durability(96)));

    public static final DeferredItem<Item> FLINT_HOE_HEAD = ITEMS.registerSimpleItem("flint_hoe_head", () -> new Item.Properties());

    public static final DeferredItem<Item> FLINT_PICKAXE = ITEMS.registerItem("flint_pickaxe", p -> new Item(p.pickaxe(ToolMaterial.STONE, 1.0F, -2.8F).durability(96)));

    public static final DeferredItem<Item> FLINT_PICKAXE_HEAD = ITEMS.registerSimpleItem("flint_pickaxe_head", () -> new Item.Properties());

    public static final DeferredItem<Item> FLINT_SHOVEL = ITEMS.registerItem("flint_shovel", p -> new ShovelItem(ToolMaterial.WOOD, 1.5F, -3.0F, p.durability(96)));

    public static final DeferredItem<Item> FLINT_SHOVEL_HEAD = ITEMS.registerSimpleItem("flint_shovel_head", () -> new Item.Properties());


    

    public static final DeferredItem<Item> PLANT_FIBER = ITEMS.registerSimpleItem("plant_fiber", () -> new Item.Properties());

    public static final DeferredItem<Item> PLANT_FIBER_BUNDLE = ITEMS.registerSimpleItem("plant_fiber_bundle", () -> new Item.Properties());

    public static final DeferredItem<Item> STRAW_BUNDLE = ITEMS.registerSimpleItem("straw_bundle", () -> new Item.Properties());

    public static final DeferredItem<Item> FLINT_KNIFE = ITEMS.registerItem("flint_knife", p -> new Item(ToolMaterial.WOOD.applySwordProperties(p, 1.5F, -1.5F).durability(64)));

    public static final DeferredItem<Item> FLINT_KNIFE_BLADE = ITEMS.registerSimpleItem("flint_knife_blade", () -> new Item.Properties());

    public static final DeferredItem<Item> FLINT_SPEAR = ITEMS.registerItem("flint_spear", p -> new Item(p.spear(ToolMaterial.WOOD, 0.65F, 0.7F, 0.75F, 5.0F, 14.0F, 10.0F, 5.1F, 15.0F, 4.6F).durability(96)));

    public static final DeferredItem<Item> FLINT_SPEAR_HEAD = ITEMS.registerSimpleItem("flint_spear_head", () -> new Item.Properties());
}