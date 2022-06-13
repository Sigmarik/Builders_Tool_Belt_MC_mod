package net.fabricmc.tbmod.mixin;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.fabricmc.tbmod.ToolData;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.command.argument.BlockStateArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.command.CommandManager;
import net.minecraft.tag.BlockTags;
import net.minecraft.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
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

    private static final Double SIZE_LIMIT = 256.0;

    private String toolMode = PAINT_MODE;
    private String toolShape = SPHERE_SHAPE;
    private double toolSize = 3.5;
    private int toolOpacity = 100;
    private boolean toolMasking = true;
    private boolean toolGradient = false;
    private boolean toolVolumeCheck = false;
    private Block toolMaterial = Registry.BLOCK.get(new Identifier("minecraft", "stone"));

    protected BuilderTool(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super((float)attackDamage, attackSpeed, material, BlockTags.PICKAXE_MINEABLE, settings);
    }

    private boolean checkMask(Block currentBlock, Block clickBlock, Block materialBlock) {
        if (toolMode.equals(ERASE_MODE)) {
            return currentBlock.equals(materialBlock);
        }
        if (toolMode.equals(BUILD_MODE)) {
            return currentBlock.equals(Blocks.AIR);
        }
        if (toolMode.equals(PAINT_MODE)) {
            return currentBlock.equals(clickBlock);
        }
        return false;
    }

    private ArrayList<BlockPos> applyCubic(World world, BlockPos origin) {
        Block clickBlock = world.getBlockState(origin).getBlock();
        ArrayList<BlockPos> answer = new ArrayList<>();
        int subtraction = ceil(toolSize / 2);
        int addition = ceil(toolSize / 2 + 1);
        if (Objects.equals(toolShape, CUBIC_SHAPE)) {
            subtraction = floor(toolSize / 2);
            addition = floor(toolSize) - subtraction;
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
                    if (toolMasking && !checkMask(world.getBlockState(new BlockPos(dx, dy, dz)).getBlock(),
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
        Block clickBlock = world.getBlockState(origin).getBlock();
        ArrayList<BlockPos> answer = new ArrayList<>();
        double x = origin.getX();
        double y = origin.getY();
        double z = origin.getZ();
        for (int iteration = 0;
             iteration <= ceil(toolSize);
             iteration++,
                     x += vector.getX(),
                     y += vector.getY(),
                     z += vector.getZ()) {
            int dx = (int) Math.round(x);
            int dy = (int) Math.round(y);
            int dz = (int) Math.round(z);
            if (dy < -64 || dy > 319) {break;}
            if (new BlockPos(dx, dy, dz).equals(origin)) {continue;}
            if (toolMasking && !checkMask(world.getBlockState(new BlockPos(dx, dy, dz)).getBlock(),
                    clickBlock, toolMaterial)) {
                if (toolVolumeCheck) {break;}
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

    private String makeLore() {
        return "{\"text\":\"" + MATERIAL_PARAMETER + " = " + Registry.BLOCK.getId(toolMaterial) + "\"}, " +
                "{\"text\":\"" + MODE_PARAMETER + " = " + toolMode + "\"}, " +
                "{\"text\":\"" + SHAPE_PARAMETER + " = " + toolShape + "\"}, " +
                "{\"text\":\"" + SIZE_PARAMETER + " = " + toolSize + "\"}, " +
                "{\"text\":\"" + OPACITY_PARAMETER + " = " + toolOpacity + "\"}, " +
                "{\"text\":\"" + MASKING_PARAMETER + " = " + toolMasking + "\"}, " +
                "{\"text\":\"" + CHECK_VOLUMES_PARAMETER + " = " + toolVolumeCheck + "\"}, " +
                "{\"text\":\"" + GRADIENT_PARAMETER + " = " + toolGradient + "\"}";
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        if (!stack.getName().getString().startsWith("TOOL/")) {return;}
        tooltip.add(Text.of(MATERIAL_PARAMETER + " = " + Registry.BLOCK.getId(toolMaterial)));
        tooltip.add(Text.of(MODE_PARAMETER + " = " + toolMode));
        tooltip.add(Text.of(SHAPE_PARAMETER + " = " + toolShape));
        tooltip.add(Text.of(SIZE_PARAMETER + " = " + toolSize));
        tooltip.add(Text.of(OPACITY_PARAMETER + " = " + toolOpacity));
        tooltip.add(Text.of(MASKING_PARAMETER + " = " + toolMasking));
        tooltip.add(Text.of(CHECK_VOLUMES_PARAMETER + " = " + toolVolumeCheck));
        tooltip.add(Text.of(GRADIENT_PARAMETER + " = " + toolGradient));
    }

    private String makeNBT() {
        return MATERIAL_PARAMETER + ":\"" + Registry.BLOCK.getId(toolMaterial) + "\"," +
                MODE_PARAMETER + ":\"" + toolMode + "\"," +
                SHAPE_PARAMETER + ":\"" + toolShape + "\"," +
                SIZE_PARAMETER + ":" + toolSize + "," +
                OPACITY_PARAMETER + ":" + toolOpacity + "," +
                MASKING_PARAMETER + ":" + toolMasking + "," +
                CHECK_VOLUMES_PARAMETER + ":" + toolVolumeCheck + "," +
                GRADIENT_PARAMETER + ":" + toolGradient;
    }

    private void writeNbt(NbtCompound nbt) {
        nbt.putString(MATERIAL_PARAMETER, Registry.BLOCK.getId(toolMaterial).toString());
        nbt.putString(MODE_PARAMETER, toolMode);
        nbt.putString(SHAPE_PARAMETER, toolShape);
        nbt.putDouble(SIZE_PARAMETER, toolSize);
        nbt.putInt(OPACITY_PARAMETER, toolOpacity);
        nbt.putBoolean(MASKING_PARAMETER, toolMasking);
        nbt.putBoolean(CHECK_VOLUMES_PARAMETER, toolVolumeCheck);
        nbt.putBoolean(GRADIENT_PARAMETER, toolGradient);
    }

    private void readNBT(NbtCompound nbt) {
        NbtCompound tag = nbt.getCompound("tag");
        if (!tag.contains(MATERIAL_PARAMETER)) {return;}
        String materialValue = tag.getString(MATERIAL_PARAMETER);
        if (materialValue.length() > 0) {
            this.toolMaterial = Registry.BLOCK.get(
                    new Identifier(materialValue.split(":")[0], materialValue.split(":")[1]));
        }
        this.toolMode = tag.getString(MODE_PARAMETER);
        this.toolShape = tag.getString(SHAPE_PARAMETER);
        this.toolSize = Math.min(tag.getDouble(SIZE_PARAMETER), SIZE_LIMIT);
        this.toolOpacity = tag.getInt(OPACITY_PARAMETER);
        this.toolMasking = tag.getBoolean(MASKING_PARAMETER);
        this.toolVolumeCheck = tag.getBoolean(CHECK_VOLUMES_PARAMETER);
        this.toolGradient = tag.getBoolean(GRADIENT_PARAMETER);
    }

    private void parseName(String name) {
        if (!name.startsWith("TOOL/")) {return;}
        if (name.equals("TOOL/")) {return;}
        String[] commands = name.split("/")[1].split(";");
        for (String command : commands) {
            if (!command.contains("=")) {continue;}
            command = command.replaceAll(" ", "");
            String argument = command.split("=")[0];
            String value = command.split("=")[1];
            if (Objects.equals(argument, MATERIAL_PARAMETER)) {
                if (value.contains(":")) {
                    this.toolMaterial = Registry.BLOCK.get(
                            new Identifier(value.split(":")[0], value.split(":")[1]));
                } else {
                    this.toolMaterial = Registry.BLOCK.get(new Identifier("minecraft", value));
                }
            }
            if (Objects.equals(argument, MODE_PARAMETER)) {this.toolMode = value;}
            if (Objects.equals(argument, SHAPE_PARAMETER)) {this.toolShape = value;}
            if (Objects.equals(argument, SIZE_PARAMETER)) {this.toolSize = Math.min(Double.parseDouble(value), SIZE_LIMIT);}
            if (Objects.equals(argument, OPACITY_PARAMETER)) {this.toolOpacity = Integer.parseInt(value);}
            if (Objects.equals(argument, MASKING_PARAMETER)) {this.toolMasking = Boolean.parseBoolean(value);}
            if (Objects.equals(argument, CHECK_VOLUMES_PARAMETER)) {this.toolVolumeCheck = Boolean.parseBoolean(value);}
            if (Objects.equals(argument, GRADIENT_PARAMETER)) {this.toolGradient = Boolean.parseBoolean(value);}
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (!context.getWorld().isClient()) {
            String stackName = context.getStack().getName().getString();
            if (!stackName.startsWith("TOOL/")) {return ActionResult.PASS;}
            Block clickBlock = context.getWorld().getBlockState(context.getBlockPos()).getBlock();
            readNBT(context.getStack().getNbt());
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
                        state = toolMaterial.getDefaultState();
                    }
                    context.getWorld().setBlockState(bPos, state);
                }
            } else {
                if (stackName.equals("TOOL/")) {this.toolMaterial = clickBlock;}
                parseName(stackName);
                context.getStack().setCustomName(Text.of("TOOL/"));
                writeNbt(context.getStack().getNbt());
            }
        }
        return ActionResult.SUCCESS;
    }
}
