package net.fabricmc.tbmod.items;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;

public class PasteTool extends Item {
    public PasteTool(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getPlayer().isCreativeLevelTwoOp()) {return ActionResult.PASS;}
        ItemStack chipStack = context.getPlayer().getStackInHand(Hand.OFF_HAND);
        if (!chipStack.getItem().getClass().equals(ZoneChip.class)) {return ActionResult.PASS;}
        ZoneChip chip = (ZoneChip) chipStack.getItem();
        chip.readNbt(chipStack);
        ArrayList<BlockPos> chipData = chip.selection;
        if (chipData.size() % 2 == 0) {return ActionResult.FAIL;}
        BlockPos origin = chip.points.get(chip.points.size() - 1);
        BlockPos clickPos = context.getBlockPos();
        if (!context.getWorld().isClient) {
            Block[] hollowBlocks = {Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR};
            for (int iterator = 0; iterator + 1 < chipData.size(); iterator += 2) {
                BlockPos minCorner = chipData.get(iterator);
                BlockPos maxCorner = chipData.get(iterator + 1);
                for (int absX = minCorner.getX(); absX <= maxCorner.getX(); absX++) {
                    for (int absY = minCorner.getY(); absY <= maxCorner.getY(); absY++) {
                        for (int absZ = minCorner.getZ(); absZ <= maxCorner.getZ(); absZ++) {
                            int relativeX = absX - origin.getX();
                            int relativeY = absY - origin.getY();
                            int relativeZ = absZ - origin.getZ();
                            BlockPos newPosition = new BlockPos(relativeX + clickPos.getX(),
                                    relativeY + clickPos.getY(),
                                    relativeZ + clickPos.getZ());
                            BlockState pasteState = context.getWorld().getBlockState(new BlockPos(absX, absY, absZ));
                            boolean isHollow = false;
                            for (Block hollowBlock : hollowBlocks) {
                                if (pasteState.getBlock().equals(hollowBlock)) {isHollow = true;}
                            }
                            if (isHollow) {continue;}
                            context.getWorld().setBlockState(newPosition, pasteState);
                        }
                    }
                }
            }
        }
        return ActionResult.SUCCESS;
    }
}
