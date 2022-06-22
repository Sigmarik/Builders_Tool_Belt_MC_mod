package net.fabricmc.tbmod.items;

import net.fabricmc.tbmod.util.ToolSettings;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.Math.abs;
import static java.lang.Math.round;
import static net.minecraft.util.math.MathHelper.floor;

public class MultiTool extends Item {

    private ToolSettings globalSettings = new ToolSettings();

    private final HashSet<BlockPos> visitedBlocks = new HashSet<>();

    public MultiTool(Settings settings) {
        super(settings);
    }

    private boolean checkMask(BlockState currentBlock, BlockState clickBlock, BlockState materialBlock) {
        if (globalSettings.matchState) {
            if (globalSettings.mode.equals(ToolSettings.ERASE_MODE)) {
                return currentBlock.equals(materialBlock);
            }
            if (globalSettings.mode.equals(ToolSettings.BUILD_MODE)) {
                return currentBlock.equals(Blocks.AIR.getDefaultState());
            }
            if (globalSettings.mode.equals(ToolSettings.PAINT_MODE)) {
                return currentBlock.equals(clickBlock);
            }
        } else {
            if (globalSettings.mode.equals(ToolSettings.ERASE_MODE)) {
                return currentBlock.getBlock().equals(materialBlock.getBlock());
            }
            if (globalSettings.mode.equals(ToolSettings.BUILD_MODE)) {
                return currentBlock.getBlock().equals(Blocks.AIR);
            }
            if (globalSettings.mode.equals(ToolSettings.PAINT_MODE)) {
                return currentBlock.getBlock().equals(clickBlock.getBlock());
            }
        }
        return false;
    }

    private void dfs(World world, BlockPos position, BlockPos clickPos, int depth) {
        if (depth > 1000) return;
        int subtraction = floor(Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT) / 2);
        int addition = floor(Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT)) - subtraction - 1;
        double delta = Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT);
        if (globalSettings.shape.equals(ToolSettings.CUBIC_SHAPE)) delta *= Math.sqrt(3.0) / 2;
        BlockState clickBlock = world.getBlockState(clickPos);
        if (visitedBlocks.contains(position)) return;
        if (globalSettings.masking && !checkMask(world.getBlockState(position), clickBlock, globalSettings.material)) return;
        if (!globalSettings.mode.equals(ToolSettings.BUILD_MODE) && world.getBlockState(position).getBlock().equals(Blocks.AIR)) return;
        if (globalSettings.shape.equals(ToolSettings.SPHERE_SHAPE) &&
                position.getSquaredDistance(clickPos) > delta * delta / 4.0) {return;}
        if (globalSettings.shape.equals(ToolSettings.CUBIC_SHAPE) &&
                (position.getX() - clickPos.getX() > addition ||
                        position.getY() - clickPos.getY() > addition ||
                        position.getZ() - clickPos.getZ() > addition ||
                        clickPos.getX() - position.getX() > subtraction ||
                        clickPos.getY() - position.getY() > subtraction ||
                        clickPos.getZ() - position.getZ() > subtraction)) {return;}
        this.visitedBlocks.add(position);
        BlockPos[] available = {position.up(), position.down(),
                position.south(), position.north(),
                position.east(), position.west()};
        for (BlockPos newPos : available) {
            dfs(world, newPos, clickPos, depth + 1);
        }
    }

    private ArrayList<BlockPos> applyCubic(World world, BlockPos origin, ItemUsageContext context) {
        BlockState clickBlock = world.getBlockState(origin);
        ArrayList<BlockPos> answer = new ArrayList<>();
        int subtraction = (int) round(Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT) / 2 + 1);
        int addition = (int) round(Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT) / 2 + 1);
        if (Objects.equals(globalSettings.shape, ToolSettings.CUBIC_SHAPE)) {
            subtraction = floor(Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT) / 2);
            addition = floor(Math.min(globalSettings.size, ToolSettings.CUBIC_SIZE_LIMIT)) - subtraction - 1;
        }
        if (globalSettings.volumeCheck) {
            this.visitedBlocks.clear();
            BlockPos position = origin;
            if (globalSettings.mode.equals(ToolSettings.BUILD_MODE)) {
                Vec3f normal = context.getSide().getUnitVector();
                position = new BlockPos(Math.round(position.getX() + normal.getX()),
                        Math.round(position.getY() + normal.getY()),
                        Math.round(position.getZ() + normal.getZ()));
            }
            dfs(world, position, origin, 0);
        }
        for (int dx = origin.getX() - subtraction; dx <= origin.getX() + addition; dx++) {
            for (int dy = origin.getY() - subtraction; dy <= origin.getY() + addition; dy++) {
                for (int dz = origin.getZ() - subtraction; dz <= origin.getZ() + addition; dz++) {
                    BlockPos curentPos = new BlockPos(dx, dy, dz);
                    double distance = Math.sqrt(
                            (dx - origin.getX()) * (dx - origin.getX()) +
                                    (dy - origin.getY()) * (dy - origin.getY()) +
                                    (dz - origin.getZ()) * (dz - origin.getZ()));
                    if (globalSettings.shape.equals(ToolSettings.CUBIC_SHAPE)) {
                        distance = Math.max(abs(dx - origin.getX()),
                                Math.max(abs(dy - origin.getY()),
                                        abs(dz - origin.getZ())));
                    }
                    int percentage = (int) round((1.0 - distance / (globalSettings.size / 2.0)) * globalSettings.opacity);
                    if (!globalSettings.gradient) {percentage=globalSettings.opacity;}
                    if (dy < -64 || dy > 319) {continue;}
                    if (globalSettings.shape.equals(ToolSettings.SPHERE_SHAPE) && distance > globalSettings.size / 2.0) {continue;}
                    if (globalSettings.masking && !checkMask(world.getBlockState(curentPos),
                            clickBlock, globalSettings.material)) {continue;}
                    int rand = ThreadLocalRandom.current().nextInt(0, 100);
                    if (rand >= percentage) {continue;}
                    if (globalSettings.volumeCheck && !visitedBlocks.contains(curentPos)) {continue;}
                    if (globalSettings.surfaceOnly && !isSurface(world, new BlockPos(dx, dy, dz), globalSettings.mode)) {continue;}
                    answer.add(curentPos);
                }
            }
        }
        return answer;
    }

    private ArrayList<BlockPos> applyLine(World world, BlockPos origin, Vec3d vector) {
        BlockState clickBlock = world.getBlockState(origin);
        ArrayList<BlockPos> answer = new ArrayList<>();
        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();
        int sgn = (int) Math.signum(globalSettings.size);
        int maxIterator = (int) round(Math.min(abs(globalSettings.size), ToolSettings.LINEAR_SIZE_LIMIT));
        if (sgn > 0) {maxIterator++;}
        for (int iteration = 0;
             iteration < maxIterator;
             iteration++,
                     x += vector.getX() * sgn,
                     y += vector.getY() * sgn,
                     z += vector.getZ() * sgn) {
            int dx = (int) Math.round(x);
            int dy = (int) Math.round(y);
            int dz = (int) Math.round(z);
            if (dy < -64 || dy > 319) {break;}
            boolean affectOrigin = (sgn < 0 && Objects.equals(globalSettings.shape, ToolSettings.BEAM_SHAPE)) ||
                    (sgn > 0 && Objects.equals(globalSettings.shape, ToolSettings.LINE_SHAPE));
            if (new BlockPos(dx, dy, dz).equals(origin) && !affectOrigin) {continue;}
            if (globalSettings.masking && !checkMask(world.getBlockState(new BlockPos(dx, dy, dz)),
                    clickBlock, globalSettings.material)) {
                if (globalSettings.volumeCheck && !(new BlockPos(dx, dy, dz).equals(origin))) {break;}
                else {continue;}
            }
            int rand = ThreadLocalRandom.current().nextInt(0, 100);
            int percentage = (int) round((1.0 - iteration / globalSettings.size) * globalSettings.opacity);
            if (!globalSettings.gradient) {percentage=globalSettings.opacity;}
            if (rand >= percentage) {continue;}
            if (globalSettings.surfaceOnly && !isSurface(world, new BlockPos(dx, dy, dz), globalSettings.mode)) {continue;}
            answer.add(new BlockPos(dx, dy, dz));
        }
        return answer;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        globalSettings = new ToolSettings(stack);
        globalSettings.applyCommand(stack.getName().getString());
        stack.setCustomName(Text.of(stack.getName().getString().split("/")[0]));
        globalSettings.writeTo(stack);
        tooltip.add(Text.of(ToolSettings.MATERIAL_PARAMETER + " = " + this.globalSettings.material.getBlock().getName().getString()));
        tooltip.add(Text.of(ToolSettings.MODE_PARAMETER + " = " + this.globalSettings.mode));
        tooltip.add(Text.of(ToolSettings.SHAPE_PARAMETER + " = " + this.globalSettings.shape));
        String sizeValue = Double.toString(this.globalSettings.size);
        if (!(this.globalSettings.minSize == 0.0 && this.globalSettings.maxSize == 0.0)) {
            sizeValue = "(" + this.globalSettings.minSize + ", " + this.globalSettings.maxSize + ")";
        }
        tooltip.add(Text.of(ToolSettings.SIZE_PARAMETER + " = " + sizeValue));
        tooltip.add(Text.of(ToolSettings.OPACITY_PARAMETER + " = " + this.globalSettings.opacity));
        tooltip.add(Text.of(ToolSettings.MASKING_PARAMETER + " = " + this.globalSettings.masking));
        tooltip.add(Text.of(ToolSettings.CHECK_VOLUMES_PARAMETER + " = " + this.globalSettings.volumeCheck));
        tooltip.add(Text.of(ToolSettings.GRADIENT_PARAMETER + " = " + this.globalSettings.gradient));
        tooltip.add(Text.of(ToolSettings.STATE_MATCH_PARAMETER + " = " + this.globalSettings.matchState));
        tooltip.add(Text.of(ToolSettings.SURFACE_PARAMETER + " = " + this.globalSettings.surfaceOnly));
    }

    private ArrayList<BlockPos> readZones(ItemStack stack) {
        ArrayList<BlockPos> answer = new ArrayList<>();
        if (stack == null || !stack.getItem().getClass().equals(ZoneChip.class)) return answer;
        ZoneChip chip = (ZoneChip) stack.getItem();
        chip.readNbt(stack);
        return chip.selection;
    }

    private boolean checkSelection(ArrayList<BlockPos> selection, BlockPos position) {
        if (selection.size() == 0) return true;
        for (int i = 0; i + 1 < selection.size(); i += 2) {
            BlockPos minC = selection.get(i);
            BlockPos maxC = selection.get(i + 1);
            if (minC.getX() <= position.getX() && position.getX() <= maxC.getX() &&
                    minC.getY() <= position.getY() && position.getY() <= maxC.getY() &&
                    minC.getZ() <= position.getZ() && position.getZ() <= maxC.getZ()) return true;
        }
        return false;
    }

    private boolean isSurface(World world, BlockPos position, String mode) {
        BlockPos[] available = {position.up(), position.down(),
                position.south(), position.north(),
                position.east(), position.west()};
        Block[] emptyBlocks = {Blocks.AIR, Blocks.VOID_AIR, Blocks.CAVE_AIR,
                Blocks.STRUCTURE_VOID, Blocks.WATER, Blocks.LAVA, Blocks.LIGHT};
        for (BlockPos bPos : available) {
            Block block = world.getBlockState(bPos).getBlock();
            boolean isEmpty = false;
            for (Block emptyBlock : emptyBlocks) {
                isEmpty = isEmpty || block == emptyBlock;
            }
            if (mode.equals(ToolSettings.BUILD_MODE) && !isEmpty) {
                return true;
            }
            if (!mode.equals(ToolSettings.BUILD_MODE) && isEmpty) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        String stackName = context.getStack().getName().getString();
        if (!context.getPlayer().isCreativeLevelTwoOp()) {return ActionResult.PASS;}
        globalSettings = new ToolSettings(context.getStack());
        if (!context.getWorld().isClient()) {
            BlockState clickBlock = context.getWorld().getBlockState(context.getBlockPos());
            if (!player.isSneaking()) {
                ArrayList<BlockPos> points = null;
                ArrayList<BlockPos> selection = readZones(context.getPlayer().getStackInHand(Hand.OFF_HAND));
                if (Objects.equals(globalSettings.shape, ToolSettings.SPHERE_SHAPE) || Objects.equals(globalSettings.shape, ToolSettings.CUBIC_SHAPE)) {
                    //MinecraftClient.getInstance().player.sendChatMessage("CUBIC");
                    points = applyCubic(context.getWorld(), context.getBlockPos(), context);
                }
                if (Objects.equals(globalSettings.shape, ToolSettings.LINE_SHAPE) || Objects.equals(globalSettings.shape, ToolSettings.BEAM_SHAPE)) {
                    //MinecraftClient.getInstance().player.sendChatMessage("LINEAR");
                    Vec3d vector = context.getHitPos().subtract(context.getPlayer().getEyePos()).normalize();
                    if (Objects.equals(globalSettings.shape, ToolSettings.BEAM_SHAPE)) {
                        vector = new Vec3d(context.getSide().getUnitVector());
                    }
                    points = applyLine(context.getWorld(), context.getBlockPos(), vector);
                }
                //MinecraftClient.getInstance().player.sendChatMessage("Length -> " + points.size());
                assert points != null;
                for (BlockPos bPos : points) {
                    if (!checkSelection(selection, bPos)) {continue;}
                    BlockState state;
                    if (Objects.equals(globalSettings.mode, ToolSettings.ERASE_MODE)) {
                        state = Blocks.AIR.getDefaultState();
                    } else {
                        if (globalSettings.mode.equals(ToolSettings.PAINT_MODE) &&
                                context.getWorld().getBlockState(bPos).getBlock().equals(Blocks.AIR)) continue;
                        state = globalSettings.material;
                    }
                    context.getWorld().setBlockState(bPos, state);
                }
            } else {
                if (!stackName.contains("/")) {this.globalSettings.material = clickBlock;}
                //MinecraftClient.getInstance().player.sendChatMessage(clickBlock.toString());
                globalSettings.applyCommand(stackName);
                context.getStack().setCustomName(Text.of(stackName.split("/")[0]));
                globalSettings.writeTo(context.getStack());
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (user.isSneaking()) {
            user.openHandledScreen(createScreenHandlerFactory(user.getBlockStateAtPos(), world, user.getBlockPos()));
        } else {
            if (world.isClient) {
                double range = 1024.0;
                BlockHitResult traceHit = longRangeRaycast(world, user, RaycastContext.FluidHandling.NONE, 1024.0);
                String message = "Distance: " + Math.sqrt(traceHit.squaredDistanceTo(user));
                if (Math.sqrt(traceHit.squaredDistanceTo(user)) > range - 1.0) message = "Distance: inf";
                user.sendMessage(Text.of(message), true);
            }
        }
        return super.use(world, user, hand);
    }

    private static BlockHitResult longRangeRaycast(World world, PlayerEntity player,
                                                   RaycastContext.FluidHandling fluidHandling, double distance) {
        float f = player.getPitch();
        float g = player.getYaw();
        Vec3d vec3d = player.getEyePos();
        float h = MathHelper.cos(-g * ((float)Math.PI / 180) - (float)Math.PI);
        float i = MathHelper.sin(-g * ((float)Math.PI / 180) - (float)Math.PI);
        float j = -MathHelper.cos(-f * ((float)Math.PI / 180));
        float k = MathHelper.sin(-f * ((float)Math.PI / 180));
        float l = i * j;
        float m = k;
        float n = h * j;
        double d = distance;
        Vec3d vec3d2 = vec3d.add((double)l * distance, (double)m * distance, (double)n * distance);
        return world.raycast(new RaycastContext(vec3d, vec3d2, RaycastContext.ShapeType.OUTLINE, fluidHandling, player));
    }

    private NamedScreenHandlerFactory createScreenHandlerFactory(BlockState state, World world, BlockPos pos) {
        return new SimpleNamedScreenHandlerFactory((syncId, inventory, player) ->
                new AnvilScreenHandler(syncId, inventory, ScreenHandlerContext.EMPTY), Text.of("Edit tool"));
    }

    @Override
    public boolean onStackClicked(ItemStack stack, Slot slot, ClickType clickType, PlayerEntity player) {
        ToolSettings settings = new ToolSettings(stack);
        settings.applyCommand(stack.getName().getString());
        stack.setCustomName(Text.of(stack.getName().getString().split("/")[0]));
        settings.writeTo(stack);
        return false;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) { return stack.hasNbt(); }

    @Override
    public int getItemBarStep(ItemStack stack) {
        ToolSettings settings = new ToolSettings(stack);
        return (int) Math.round(13.0f * settings.opacity / 100.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        ToolSettings settings = new ToolSettings(stack);
        float multiplyer = 0.5F;
        if (settings.masking) multiplyer = 1.0F;
        return MathHelper.multiplyColors(getRarity(stack).formatting.getColorValue(), multiplyer, multiplyer, multiplyer);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        ToolSettings settings = new ToolSettings(stack);
        if (settings.mode.equals(ToolSettings.BUILD_MODE)) {
            return Rarity.UNCOMMON;
        }
        if (settings.mode.equals(ToolSettings.ERASE_MODE)) {
            return Rarity.EPIC;
        }
        if (settings.mode.equals(ToolSettings.PAINT_MODE)) {
            return Rarity.RARE;
        }
        return super.getRarity(stack);
    }
}
