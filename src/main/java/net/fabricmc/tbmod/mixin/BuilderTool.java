package net.fabricmc.tbmod.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.screen.*;
import net.minecraft.screen.slot.Slot;
import net.minecraft.tag.BlockTags;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.MathHelper.ceil;
import static net.minecraft.util.math.MathHelper.floor;

@Mixin(PickaxeItem.class)
public class BuilderTool extends MiningToolItem {

    private static final String ERASE_MODE = "erase";
    private static final String BUILD_MODE = "build";
    private static final String PAINT_MODE = "paint";

    private static final String SPHERE_SHAPE = "sphere";
    private static final String CUBIC_SHAPE = "cube";
    private static final String LINE_SHAPE = "line";
    private static final String BEAM_SHAPE = "beam";

    private static final String MATERIAL_PARAMETER = "material";
    private static final String MODE_PARAMETER = "mode";
    private static final String SHAPE_PARAMETER = "shape";
    private static final String SIZE_PARAMETER = "size";
    private static final String OPACITY_PARAMETER = "opacity";
    private static final String MASKING_PARAMETER = "masking";
    private static final String CHECK_VOLUMES_PARAMETER = "volume_check";
    private static final String GRADIENT_PARAMETER = "gradient";
    private static final String STATE_MATCH_PARAMETER = "match_state";

    private static final Double CUBIC_SIZE_LIMIT = 256.0;
    private static final Double LINEAR_SIZE_LIMIT = 1024.0;

    private String toolMode = PAINT_MODE;
    private String toolShape = SPHERE_SHAPE;
    private double toolSize = 3.5;
    private int toolOpacity = 100;
    private boolean toolMasking = true;
    private boolean toolGradient = false;
    private boolean toolVolumeCheck = false;
    private BlockState toolMaterial = Blocks.STONE.getDefaultState();
    private boolean toolMatchState = false;

    protected BuilderTool(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super((float)attackDamage, attackSpeed, material, BlockTags.PICKAXE_MINEABLE, settings);
    }

    private boolean checkMask(BlockState currentBlock, BlockState clickBlock, BlockState materialBlock) {
        if (toolMatchState) {
            if (toolMode.equals(ERASE_MODE)) {
                return currentBlock.equals(materialBlock);
            }
            if (toolMode.equals(BUILD_MODE)) {
                return currentBlock.equals(Blocks.AIR.getDefaultState());
            }
            if (toolMode.equals(PAINT_MODE)) {
                return currentBlock.equals(clickBlock);
            }
        } else {
            if (toolMode.equals(ERASE_MODE)) {
                return currentBlock.getBlock().equals(materialBlock.getBlock());
            }
            if (toolMode.equals(BUILD_MODE)) {
                return currentBlock.getBlock().equals(Blocks.AIR);
            }
            if (toolMode.equals(PAINT_MODE)) {
                return currentBlock.getBlock().equals(clickBlock.getBlock());
            }
        }
        return false;
    }

    private ArrayList<BlockPos> applyCubic(World world, BlockPos origin) {
        BlockState clickBlock = world.getBlockState(origin);
        ArrayList<BlockPos> answer = new ArrayList<>();
        int subtraction = ceil(Math.min(toolSize, CUBIC_SIZE_LIMIT) / 2);
        int addition = ceil(Math.min(toolSize, CUBIC_SIZE_LIMIT) / 2 + 1);
        if (Objects.equals(toolShape, CUBIC_SHAPE)) {
            subtraction = floor(Math.min(toolSize, CUBIC_SIZE_LIMIT) / 2);
            addition = floor(Math.min(toolSize, CUBIC_SIZE_LIMIT)) - subtraction;
        }
        for (int dx = origin.getX() - subtraction; dx <= origin.getX() + addition; dx++) {
            for (int dy = origin.getY() - subtraction; dy <= origin.getY() + addition; dy++) {
                for (int dz = origin.getZ() - subtraction; dz <= origin.getZ() + addition; dz++) {
                    double distance = Math.sqrt(
                                    (dx - origin.getX()) * (dx - origin.getX()) +
                                    (dy - origin.getY()) * (dy - origin.getY()) +
                                    (dz - origin.getZ()) * (dz - origin.getZ()));
                    if (toolShape.equals(CUBIC_SHAPE)) {
                        distance = Math.max(Math.abs(dx - origin.getX()),
                                Math.max(Math.abs(dy - origin.getY()),
                                        Math.abs(dz - origin.getZ())));
                    }
                    int percentage = ceil((1.0 - distance / (toolSize / 2.0)) * toolOpacity);
                    if (!toolGradient) {percentage=toolOpacity;}
                    if (dy < -64 || dy > 319) {continue;}
                    if (toolShape.equals(SPHERE_SHAPE) && distance > toolSize / 2.0) {continue;}
                    if (toolMasking && !checkMask(world.getBlockState(new BlockPos(dx, dy, dz)),
                            clickBlock, toolMaterial)) {continue;}
                    int rand = ThreadLocalRandom.current().nextInt(0, 100);
                    if (rand >= percentage) {continue;}
                    answer.add(new BlockPos(dx, dy, dz));
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
        int sgn = (int) Math.signum(toolSize);
        for (int iteration = 0;
             iteration < ceil(Math.min(Math.abs(toolSize), LINEAR_SIZE_LIMIT));
             iteration++,
                     x += vector.getX() * sgn,
                     y += vector.getY() * sgn,
                     z += vector.getZ() * sgn) {
            int dx = (int) Math.round(x);
            int dy = (int) Math.round(y);
            int dz = (int) Math.round(z);
            if (dy < -64 || dy > 319) {break;}
            boolean affectOrigin = (sgn < 0 && Objects.equals(toolShape, BEAM_SHAPE)) ||
                    (sgn > 0 && Objects.equals(toolShape, LINE_SHAPE));
            if (new BlockPos(dx, dy, dz).equals(origin) && !affectOrigin) {continue;}
            if (toolMasking && !checkMask(world.getBlockState(new BlockPos(dx, dy, dz)),
                    clickBlock, toolMaterial)) {
                if (toolVolumeCheck && !(new BlockPos(dx, dy, dz).equals(origin))) {break;}
                else {continue;}
            }
            int rand = ThreadLocalRandom.current().nextInt(0, 100);
            int percentage = ceil((1.0 - iteration / toolSize) * toolOpacity);
            if (!toolGradient) {percentage=toolOpacity;}
            if (rand >= percentage) {continue;}
            answer.add(new BlockPos(dx, dy, dz));
        }
        return answer;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!stack.getName().getString().contains("/")) {return;}
        readNBT(stack.getNbt());
        parseName(stack.getName().getString());
        stack.setCustomName(Text.of(stack.getName().getString().split("/")[0] + "/"));
        writeNbt(stack.getNbt());
        tooltip.add(Text.of(MATERIAL_PARAMETER + " = " + this.toolMaterial.getBlock().getName().getString()));
        tooltip.add(Text.of(MODE_PARAMETER + " = " + this.toolMode));
        tooltip.add(Text.of(SHAPE_PARAMETER + " = " + this.toolShape));
        tooltip.add(Text.of(SIZE_PARAMETER + " = " + this.toolSize));
        tooltip.add(Text.of(OPACITY_PARAMETER + " = " + this.toolOpacity));
        tooltip.add(Text.of(MASKING_PARAMETER + " = " + this.toolMasking));
        tooltip.add(Text.of(CHECK_VOLUMES_PARAMETER + " = " + this.toolVolumeCheck));
        tooltip.add(Text.of(GRADIENT_PARAMETER + " = " + this.toolGradient));
        tooltip.add(Text.of(STATE_MATCH_PARAMETER + " = " + this.toolMatchState));
    }

    private void writeNbt(NbtCompound nbt) {
        nbt.put(MATERIAL_PARAMETER, NbtHelper.fromBlockState(toolMaterial));
        nbt.putString(MODE_PARAMETER, toolMode);
        nbt.putString(SHAPE_PARAMETER, toolShape);
        nbt.putDouble(SIZE_PARAMETER, toolSize);
        nbt.putInt(OPACITY_PARAMETER, toolOpacity);
        nbt.putBoolean(MASKING_PARAMETER, toolMasking);
        nbt.putBoolean(CHECK_VOLUMES_PARAMETER, toolVolumeCheck);
        nbt.putBoolean(GRADIENT_PARAMETER, toolGradient);
        nbt.putBoolean(STATE_MATCH_PARAMETER, toolMatchState);
    }

    private void readNBT(NbtCompound nbt) {
        if (!nbt.contains(MATERIAL_PARAMETER)) {return;}
        this.toolMaterial = NbtHelper.toBlockState(nbt.getCompound(MATERIAL_PARAMETER));
        this.toolMode = nbt.getString(MODE_PARAMETER);
        this.toolShape = nbt.getString(SHAPE_PARAMETER);
        this.toolSize = nbt.getDouble(SIZE_PARAMETER);
        this.toolOpacity = nbt.getInt(OPACITY_PARAMETER);
        this.toolMasking = nbt.getBoolean(MASKING_PARAMETER);
        this.toolVolumeCheck = nbt.getBoolean(CHECK_VOLUMES_PARAMETER);
        this.toolGradient = nbt.getBoolean(GRADIENT_PARAMETER);
        this.toolMatchState = nbt.getBoolean(STATE_MATCH_PARAMETER);
    }

    private void parseName(String name) {
        if (!name.contains("/")) {return;}
        if (name.endsWith("/")) {return;}
        String[] commands = name.split("/")[1].split(";");
        for (String command : commands) {
            if (!command.contains("=")) {continue;}
            command = command.replaceAll(" ", "");
            String argument = command.split("=")[0];
            String value = command.split("=")[1];
            if (Objects.equals(argument, MATERIAL_PARAMETER)) {
                if (value.contains(":")) {
                    this.toolMaterial = Registry.BLOCK.get(
                            new Identifier(value.split(":")[0], value.split(":")[1])).getDefaultState();
                } else {
                    this.toolMaterial = Registry.BLOCK.get(new Identifier("minecraft", value)).getDefaultState();
                }
            }
            if (Objects.equals(argument, MODE_PARAMETER)) {this.toolMode = value;}
            if (Objects.equals(argument, SHAPE_PARAMETER)) {this.toolShape = value;}
            if (Objects.equals(argument, SIZE_PARAMETER)) {this.toolSize = Double.parseDouble(value);}
            if (Objects.equals(argument, OPACITY_PARAMETER)) {this.toolOpacity = Integer.parseInt(value);}
            if (Objects.equals(argument, MASKING_PARAMETER)) {this.toolMasking = Boolean.parseBoolean(value);}
            if (Objects.equals(argument, CHECK_VOLUMES_PARAMETER)) {this.toolVolumeCheck = Boolean.parseBoolean(value);}
            if (Objects.equals(argument, GRADIENT_PARAMETER)) {this.toolGradient = Boolean.parseBoolean(value);}
            if (Objects.equals(argument, STATE_MATCH_PARAMETER)) {this.toolMatchState = Boolean.parseBoolean(value);}
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        String stackName = context.getStack().getName().getString();
        if (!stackName.contains("/") || !context.getPlayer().isCreativeLevelTwoOp()) {return ActionResult.PASS;}
        readNBT(context.getStack().getNbt());
        if (!context.getWorld().isClient()) {
            BlockState clickBlock = context.getWorld().getBlockState(context.getBlockPos());
            if (!player.isSneaking()) {
                ArrayList<BlockPos> points = null;
                if (Objects.equals(toolShape, SPHERE_SHAPE) || Objects.equals(toolShape, CUBIC_SHAPE)) {
                    //MinecraftClient.getInstance().player.sendChatMessage("CUBIC");
                    points = applyCubic(context.getWorld(), context.getBlockPos());
                }
                if (Objects.equals(toolShape, LINE_SHAPE) || Objects.equals(toolShape, BEAM_SHAPE)) {
                    //MinecraftClient.getInstance().player.sendChatMessage("LINEAR");
                    Vec3d vector = context.getHitPos().subtract(context.getPlayer().getEyePos()).normalize();
                    if (Objects.equals(toolShape, BEAM_SHAPE)) {
                        vector = new Vec3d(context.getSide().getUnitVector());
                    }
                    points = applyLine(context.getWorld(), context.getBlockPos(), vector);
                }
                //MinecraftClient.getInstance().player.sendChatMessage("Length -> " + points.size());
                assert points != null;
                for (BlockPos bPos : points) {
                    BlockState state;
                    if (Objects.equals(toolMode, ERASE_MODE)) {
                        state = Blocks.AIR.getDefaultState();
                    } else {
                        state = toolMaterial;
                    }
                    context.getWorld().setBlockState(bPos, state);
                }
            } else {
                if (stackName.endsWith("/")) {this.toolMaterial = clickBlock;}
                //MinecraftClient.getInstance().player.sendChatMessage(clickBlock.toString());
                parseName(stackName);
                context.getStack().setCustomName(Text.of(stackName.split("/")[0] + "/"));
                writeNbt(context.getStack().getNbt());
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!(user.getStackInHand(hand).getName().getString().contains("/") && user.isCreativeLevelTwoOp()))
            return super.use(world, user, hand);
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
        if (!stack.getName().getString().contains("/")) {return false;}
        readNBT(stack.getNbt());
        parseName(stack.getName().getString());
        stack.setCustomName(Text.of(stack.getName().getString().split("/")[0] + "/"));
        writeNbt(stack.getNbt());
        return false;
    }

    @Override
    public boolean isItemBarVisible(ItemStack stack) {
        return stack.getName().getString().contains("/") || stack.isDamaged();
    }

    @Override
    public int getItemBarStep(ItemStack stack) {
        if (!stack.getName().getString().contains("/")) {return super.getItemBarStep(stack);}
        readNBT(stack.getNbt());
        return (int) Math.round(13.0f * toolOpacity / 100.0);
    }

    @Override
    public int getItemBarColor(ItemStack stack) {
        if (!stack.getName().getString().contains("/")) {return super.getItemBarColor(stack);}
        readNBT(stack.getNbt());
        float multiplyer = 0.5F;
        if (toolMasking) multiplyer = 1.0F;
        return MathHelper.multiplyColors(getRarity(stack).formatting.getColorValue(), multiplyer, multiplyer, multiplyer);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        if (!stack.getName().getString().contains("/")) return super.getRarity(stack);
        readNBT(stack.getNbt());
        if (toolMode.equals(BUILD_MODE)) {
            return Rarity.UNCOMMON;
        }
        if (toolMode.equals(ERASE_MODE)) {
            return Rarity.EPIC;
        }
        if (toolMode.equals(PAINT_MODE)) {
            return Rarity.RARE;
        }
        return super.getRarity(stack);
    }
}
