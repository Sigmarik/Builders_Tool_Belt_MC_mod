package net.fabricmc.tbmod.items;

import net.fabricmc.fabric.api.client.itemgroup.FabricItemGroupBuilder;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.tbmod.ToolBeltMod;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class ModItems {
    public static final ItemGroup TOOLS_GROUP = FabricItemGroupBuilder.build(new Identifier(ToolBeltMod.MODID, "tools_group"),
            () -> new ItemStack(ModItems.YELLOW_PICKAXE));

    public static final ZoneChip BLUE_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final ZoneChip CYAN_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final ZoneChip DEEP_BLUE_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final ZoneChip LIME_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final ZoneChip PINK_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final ZoneChip SEMI_ORANGE_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final ZoneChip YELLOW_CHIP = new ZoneChip(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );

    public static final MultiTool BLUE_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final MultiTool CYAN_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final MultiTool DEEP_BLUE_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final MultiTool LIME_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final MultiTool PINK_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final MultiTool SEMI_ORANGE_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );
    public static final MultiTool YELLOW_PICKAXE = new MultiTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );

    public static final PasteTool PASTE_TOOL = new PasteTool(
            new FabricItemSettings().group(TOOLS_GROUP).maxCount(1).fireproof() );

    public static void registerModItems() {
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "blue_chip"), BLUE_CHIP);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "cyan_chip"), CYAN_CHIP);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "deep_blue_chip"), DEEP_BLUE_CHIP);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "lime_chip"), LIME_CHIP);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "pink_chip"), PINK_CHIP);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "semi_orange_chip"), SEMI_ORANGE_CHIP);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "yellow_chip"), YELLOW_CHIP);

        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "blue_pick"), BLUE_PICKAXE);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "cyan_pick"), CYAN_PICKAXE);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "deep_blue_pick"), DEEP_BLUE_PICKAXE);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "lime_pick"), LIME_PICKAXE);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "pink_pick"), PINK_PICKAXE);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "semi_orange_pick"), SEMI_ORANGE_PICKAXE);
        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "yellow_pick"), YELLOW_PICKAXE);

        Registry.register(Registry.ITEM, new Identifier(ToolBeltMod.MODID, "paste_tool"), PASTE_TOOL);
    }
}
