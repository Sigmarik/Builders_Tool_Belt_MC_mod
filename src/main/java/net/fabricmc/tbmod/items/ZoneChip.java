package net.fabricmc.tbmod.items;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ZoneChip extends Item {

    public final ArrayList<BlockPos> selection = new ArrayList<>();
    public final ArrayList<BlockPos> points = new ArrayList<>();

    public ZoneChip(Settings settings) {
        super(settings);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        if (!stack.hasNbt()) return false;
        readNbt(stack);
        return selection.size() > 0;
    };

    public void updateSelection() {
        this.selection.clear();
        for (int i = 0; i < this.points.size(); i++) {
            selection.add(points.get(i));
            if (i % 2 > 0) {
                BlockPos cornerA = selection.get(i - 1);
                BlockPos cornerB = selection.get(i);
                BlockPos minCorner = new BlockPos(Math.min(cornerA.getX(), cornerB.getX()),
                        Math.min(cornerA.getY(), cornerB.getY()),
                        Math.min(cornerA.getZ(), cornerB.getZ()));
                BlockPos maxCorner = new BlockPos(Math.max(cornerA.getX(), cornerB.getX()),
                        Math.max(cornerA.getY(), cornerB.getY()),
                        Math.max(cornerA.getZ(), cornerB.getZ()));
                selection.set(i - 1, minCorner);
                selection.set(i, maxCorner);
            }
        }
    }

    public void readNbt(ItemStack stack) {
        this.points.clear();
        if (!stack.hasNbt()) return;
        NbtCompound nbt = stack.getNbt();
        if (!nbt.contains("corners")) return;
        NbtList positions = nbt.getList("corners", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < positions.size(); i++) {
            this.points.add(NbtHelper.toBlockPos((NbtCompound) positions.get(i)));
        }
        updateSelection();
    }

    private void writeNbt(ItemStack stack) {
        NbtCompound nbt = new NbtCompound();
        NbtList list = new NbtList();
        for (BlockPos pos : points) {
            list.add(NbtHelper.fromBlockPos(pos));
        }
        nbt.put("corners", list);
        stack.setNbt(nbt);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient()) {
            readNbt(context.getStack());
            if (context.getPlayer().isSneaking()) {
                if (points.size() > 0) points.remove(points.size() - 1);
                else return ActionResult.FAIL;
            } else {
                if (points.size() >= 21) return ActionResult.FAIL;
                points.add(context.getBlockPos());
                updateSelection();
            }
            //context.getStack().getNbt().put("tag", new NbtCompound());
            writeNbt(context.getStack());
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        readNbt(stack);
        String suffix = " position";
        if (points.size() != 1) suffix += "s";
        tooltip.add(Text.of("Holds " + points.size() + suffix));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (world.isClient) {
            readNbt(user.getStackInHand(hand));
            for (int i = 0; i + 1 < selection.size(); i += 2) {
                BlockPos minCorner = selection.get(i);
                BlockPos maxCorner = selection.get(i + 1);
                for (int x = minCorner.getX(); x <= maxCorner.getX() + 1; x++) {
                    world.addParticle(ParticleTypes.FLAME, x, minCorner.getY(), minCorner.getZ(), 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, x, minCorner.getY(), maxCorner.getZ() + 1, 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, x, maxCorner.getY() + 1, minCorner.getZ(), 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, x, maxCorner.getY() + 1, maxCorner.getZ() + 1, 0, 0, 0);
                }
                for (int y = minCorner.getY() + 1; y < maxCorner.getY() + 1; y++) {
                    world.addParticle(ParticleTypes.FLAME, minCorner.getX(), y, minCorner.getZ(), 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, minCorner.getX(), y, maxCorner.getZ() + 1, 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, maxCorner.getX() + 1, y, minCorner.getZ(), 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, maxCorner.getX() + 1, y, maxCorner.getZ() + 1, 0, 0, 0);
                }
                for (int z = minCorner.getZ() + 1; z < maxCorner.getZ() + 1; z++) {
                    world.addParticle(ParticleTypes.FLAME, minCorner.getX(), minCorner.getY(), z, 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, minCorner.getX(), maxCorner.getY() + 1, z, 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, maxCorner.getX() + 1, minCorner.getY(), z, 0, 0, 0);
                    world.addParticle(ParticleTypes.FLAME, maxCorner.getX() + 1, maxCorner.getY() + 1, z, 0, 0, 0);
                }
            }
        }
        return super.use(world, user, hand);
    }
}
