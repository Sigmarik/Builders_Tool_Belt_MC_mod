package net.fabricmc.tbmod.mixin;

import net.fabricmc.tbmod.ToolData;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.TagKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Mixin(PickaxeItem.class)
public class BuilderTool extends MiningToolItem {
    protected BuilderTool(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super((float)attackDamage, attackSpeed, material, BlockTags.PICKAXE_MINEABLE, settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        //MinecraftClient.getInstance().player.sendCommand("");
        if (context.getWorld().isClient()) {
            BlockPos blockPos = context.getBlockPos();
            String blockName = Registry.BLOCK.getId(context.getWorld().getBlockState(blockPos).getBlock()).toString();
            String itemID = Registry.ITEM.getId(context.getStack().getItem()).toString();

            ItemStack stack = context.getStack();
            String itemName = stack.getName().getString();
            String materialID = stack.getNbt().getString("MATERIAL");
            if (!Objects.equals(itemName, "|TOOL|") && !itemName.startsWith(ToolData.PREFIX)) return ActionResult.PASS;
            ToolData data = new ToolData(itemName);
            //MinecraftClient.getInstance().player.sendChatMessage("WHATTT " + data.mode + " " + itemName);
            if (!player.isSneaking()) {
                //context.getWorld().getGameRules().getBoolean("sendCommandFeedback")
                MinecraftClient.getInstance().player.sendCommand("gamerule sendCommandFeedback false");
                int radius = (int) Math.ceil(data.size);
                for (int dx = -radius; dx <= radius; dx++) {
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dz = -radius; dz <= radius; dz++) {
                            int rand = ThreadLocalRandom.current().nextInt(0, 100);
                            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (distance > data.size) {continue;}
                            int percentage = (int) ((1.0 - distance / data.size) * (float)data.percentage);
                            if (!data.doGradient) percentage = data.percentage;
                            if (rand >= percentage) {continue;}

                            int x = context.getBlockPos().getX() + dx;
                            int y = context.getBlockPos().getY() + dy;
                            int z = context.getBlockPos().getZ() + dz;
                            if (y < -64 || y > 319) {continue;}
                            String blockCoords = Integer.toString(x) + " " + Integer.toString(y) + " " + Integer.toString(z);

                            String chosenID = Registry.BLOCK.getId(
                                    context.getWorld().getBlockState(new BlockPos(x, y, z)).getBlock()).toString();

                            if (Objects.equals(data.mode, ToolData.SCULPT_MODE) &&
                                    data.doMasking &&
                                    !Objects.equals(chosenID, "minecraft:air")) {continue;}
                            if (Objects.equals(data.mode, ToolData.ERASE_MODE) &&
                                    data.doMasking &&
                                    !Objects.equals(chosenID, materialID)) {continue;}
                            if (Objects.equals(data.mode, ToolData.BRUSH_MODE) &&
                                    data.doMasking &&
                                    !Objects.equals(chosenID, blockName)) {continue;}

                            String fillBlock = materialID;
                            if (Objects.equals(data.mode, ToolData.ERASE_MODE)) {fillBlock = "minecraft:air";}
                            MinecraftClient.getInstance().player.sendCommand("setblock " + blockCoords + " " + fillBlock);
                        }
                    }
                }
                MinecraftClient.getInstance().player.sendCommand("gamerule sendCommandFeedback true");
            } else {
                MinecraftClient.getInstance().player.sendCommand("gamerule sendCommandFeedback false");
                if (itemName.equals("|TOOL|") || true) {itemName = data.makeName();}
                MinecraftClient.getInstance().player.sendCommand("item replace entity @s weapon.mainhand with " + itemID +
                        "{display:{Name:'[{\"text\":\"" + itemName + "\"}]', Lore:['[{\"text\":\"" + blockName +
                        "\"}]']}, MATERIAL:\"" + blockName + "\"} 1");
                MinecraftClient.getInstance().player.sendCommand("gamerule sendCommandFeedback true");
            }
        }
        return ActionResult.SUCCESS;
    }
}
